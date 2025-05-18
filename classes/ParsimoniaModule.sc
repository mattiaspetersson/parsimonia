ParsimoniaModule { // release version
	var layer, row, col, key, parsimonia, keyAsset, posAsDegree, transpose, localFx, <resumePlayback, quant;
	var <module, <type, <name, server, <assets, scale, tempoClock;
	var additionalArgsArray, currentAmpValue, midiOut, beatDurBus;
	var currentParVals, currentMorphVals, <isPlaying;

	*new {|layer, row, col, key, aParsimonia, keyAsset, posAsDegree = false, transpose = 0, localFx = false, resumePlayback = false, quant = 0|
		^super.newCopyArgs(layer, row, col, key, aParsimonia, keyAsset, posAsDegree, transpose, localFx, resumePlayback, quant).init;
	}

	init {
		parsimonia.addDependant(this);
		server = parsimonia.server;
		scale = parsimonia.scale;
		tempoClock = parsimonia.tempoClock;
		beatDurBus = parsimonia.beatDurBus;
		assets = ();
		currentParVals = ();
		currentMorphVals = (morphA: (), morphB: (), morphC: ());
		isPlaying = false;
		this.initModule;
	}

	initModule { } // overwritten in each module

	loadSample {|path|
		if(path.notNil, {
			if(assets[\sampleBuffer].isNil, {
				assets[\sampleBuffer] = Buffer.read(server, path);
			}, {
				assets[\sampleBuffer].free;
				assets[\sampleBuffer] = Buffer.read(server, path);
			});
		});
	}

	pointToBuffer {|buf|
		if(buf.notNil, {
			if(assets[\sampleBuffer].isNil, {
				assets[\sampleBuffer] = buf;
			});
		});
	}

	loadVstInstr {|path, programNumber = 4|
		if(path.notNil, {
			fork{
				server.sync;
				server.bind{
					module = VSTPluginController(
						Synth(\vsti, [
							\freq, if(posAsDegree, {
								((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose);
							}, {
								48+transpose;
							}),
							\outBus, parsimonia.masterBus,
							\fxBus, parsimonia.fxBus,
							\localFxBus, parsimonia.localFxBusses[layer][row][col]
						]++additionalArgsArray, parsimonia.moduleGroups[layer][row][col])
				)};
				server.sync;
				server.bind{
					module.open(path, false, true, {|plug| plug.program = programNumber ? 1});
					midiOut = module.midi;
				};
			};
		}, {
			"Path to a vst instrument needed".error;
		});
	}

	setInput {|arrayOfInputAndGain|
		assets[\inBus] = arrayOfInputAndGain[0];
		assets[\gain] = arrayOfInputAndGain[1];
	}

	addControlSpec {|par, spec|
		if(assets[\controlSpecs].isNil, {
			assets[\controlSpecs] = ();
		});
		assets[\controlSpecs].put(par, spec);
	}

	assignToMorphGroup {|morphGroup, par, spec|
		if(assets[morphGroup].isNil, {
			assets[morphGroup] = ();
		});
		assets[morphGroup].put(par, spec);
		this.updateMorphGroups;
	}

	removeMorphGroup {|morphGroup, par|
		if(assets[morphGroup][par].notNil, {
			assets[morphGroup][par] = nil;
			currentMorphVals[morphGroup][par] = 0;
			this.updateMorphGroups;
			currentMorphVals[morphGroup][par] = nil;
		});
	}

	play {|bool, velocity|
		var samplePlayers = [
			ParsimoniaModule_SamplePlayer,
			ParsimoniaModule_SamplePlayerCF,
			ParsimoniaModule_SamplePlayerWarp
		];
		isPlaying = bool;
		switch(type,
			\synth, {
				tempoClock.schedAbs(tempoClock.nextTimeOnGrid(quant), {
					if(bool, {
						var n;
						if(samplePlayers.includes(this.class), {
							if(assets[\sampleBuffer].numChannels == 1, {
								n = (name++'_mono').asSymbol;
							}, {
								n = (name++'_stereo').asSymbol;
							});
						}, {
							n = name;
						});

						if(module.isNil, {
							server.bind{
								if(name == \sampleRecorderMono or: {name == \sampleRecorderStereo}) {this.recTimer(bool)};
								module = Synth(n, [
									\buf, assets[\sampleBuffer],
									\freq, if(posAsDegree, {
										((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose).midicps;
									}, {
										(48+transpose).midicps;
									}),
									\reTrigFreq, assets[\reTrigFreq] ? 0,
									\outBus, parsimonia.masterBus,
									\fxBus, parsimonia.fxBus,
									\localFxBus, parsimonia.localFxBusses[layer][row][col]
								]++additionalArgsArray, parsimonia.moduleGroups[layer][row][col]);
								this.updateParams;
								this.updateMorphGroups;
							};
						}, {
							if(resumePlayback, {
								module.set(\freq,
									if(posAsDegree, {
										((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose).midicps;
									}, {
										(48+transpose).midicps;
									});
								);
							});
						});
					}, {
						if(module.notNil, {
							server.bind{
								if(name == \sampleRecorderMono or: {name == \sampleRecorderStereo}) {this.recTimer(bool)};
								if(resumePlayback,
									{parsimonia.moduleGroups[layer][row][col].set(\freq, 0)},
									{parsimonia.moduleGroups[layer][row][col].set(\gate, 0); module = nil;}
								);
							};
						});
					});
				});
			},

			\vstInstr, {
				tempoClock.schedAbs(tempoClock.nextTimeOnGrid(quant), {
					if(bool, {
						midiOut.noteOn(
							0,
							if(posAsDegree, {
								((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose);
							}, {
								48+transpose;
							}),
							127
						);
						this.updateParams;
						this.updateMorphGroups;
					}, {
						midiOut.noteOff(
							0,
							if(posAsDegree, {
								((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose);
							}, {
								48+transpose;
							}),
							0
						);
					});
				});
			},

			\fx, {
				tempoClock.schedAbs(tempoClock.nextTimeOnGrid(quant), {
					if(bool and: {name.notNil}, {
						if(module.isNil, {
							server.bind{
								if(name == \repeater, {assets[\repeaterRecorderSynth].set(\rec, 0)});
								module = Synth(name, [
									\freq, if(posAsDegree, {
										((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose).midicps;
									}, {
										(48+transpose).midicps;
									}),
									\inBus, if(localFx.not, {parsimonia.fxBus}, {parsimonia.localFxBusses[layer][row][col]}),
									\outBus, parsimonia.masterBus,
									\beatDurInSecs, beatDurBus.asMap
								]++additionalArgsArray,
								if(name == \repeater,
									{assets[\repeaterGroup]},
									{if(localFx.not, {parsimonia.fxGroup}, {parsimonia.localFxGroups[layer][row][col]})}
								),
								\addToTail
								);
								this.updateParams;
								this.updateMorphGroups;
							};
						});
					}, {
						if(module.notNil, {
							server.bind{
								if(name == \repeater, {
									assets[\repeaterBuf].zero;
									assets[\repeaterRecorderSynth].set(\rec, 1)
								});
								module.set(\gate, 0);
							};
							module = nil;
						});
					});
				});
			},

			\vstFx, {
				// ToDo!
			},

			\midi, {
				tempoClock.schedAbs(tempoClock.nextTimeOnGrid(quant), {
					switch(name,
						\midiNote, {
							var note;
							if(posAsDegree, {
								note = ((col+(row*8)).degreeToKey(scale, scale.stepsPerOctave)+48+transpose);
							}, {
								note = assets[\midiNoteNum]+48+transpose;
							});
							if(bool, {
								assets[\midiPort].noteOn(
									assets[\midiChannel],
									note,
									velocity.lincurve(0, 98, 1, 127, -2)
								);
							}, {
								assets[\midiPort].noteOff(
									assets[\midiChannel],
									note,
									0
								);
							})
						},

						\midiCtrl, {
							if(bool, {
								assets[\midiPort].control(
									assets[\midiChannel],
									assets[\midiCtrlNum],
									assets[\midiCtrlVal].value() // could be a function. Args should be l, c, r, val!
								);
							});
						}
					);
				});
			},

			\pattern, {
				if(bool, {
					if(assets[\pattern].notNil, {
						if(module.isNil)
						{module = assets[\pattern].play(tempoClock, quant: quant)}
						{module.resume(tempoClock, quant)};
					});
					this.updateParams;
					this.updateMorphGroups;
				}, {
					if(module.notNil, {
						module.pause;
					});
				});
			}
		);
	}

	free {
		this.dependants.do{|obj| this.removeDependant(obj)};
		switch(type,
			\synth, {
				if(module.notNil, {module.free; module = nil});
				if(assets.notEmpty, {
					assets.keysValuesDo{|key, val|
						switch(val.class.asSymbol,
							\Buffer, {val.free},
							\Synth, {val.free},
							\Bus, {val.free},
							\Group, {val.free}
						);
					};
				});
			},

			\sample, {
				// probably not needed. use \synth.
			},

			\vstInstr, {
				if(module.notNil, {
					server.bind{
						midiOut.allNotesOff(0);
						module.close;
						module.synth.free;
						module = nil;
					};
				});
			},

			\fx, {
				if(module.notNil, {module.free; module = nil});
				if(assets.notEmpty, {
					assets.keysValuesDo{|key, val|
						switch(val.class.asSymbol,
							\Buffer, {val.free},
							\Synth, {val.free},
							\Bus, {val.free},
							\Group, {val.free}
						);
					};
				});
			},

			\vstFx, {

			},

			\midi, {

			}
		);
	}

	setModulePar {|par, val|
		currentParVals[par] = val;
		this.updateParams;
	}

	setMorph {|morphGroup, val|
		if(assets[morphGroup].notNil, {
			assets[morphGroup].keysValuesDo{|par, spec|
				currentMorphVals[morphGroup][par] = spec.map(val);
			};
		});
		this.updateMorphGroups;
	}

	updateParams {
		var morphVal = 0;
		currentParVals.keysValuesDo{|par, val|
			[\morphA, \morphB, \morphC].do{|m|
				morphVal = morphVal + (currentMorphVals[m][par] ? 0);
			};
			switch(type,
				\synth, {server.bind{module.set(par, val)}},

				\pattern, {server.bind{parsimonia.moduleGroups[layer][row][col].set(par, val)}},

				\vstInstr, {server.bind{parsimonia.moduleGroups[layer][row][col].set(par, val + morphVal)}},

				\fx, {server.bind{module.set(par, val)}},

				\vstFx, { },

				\midi, {
					switch(name,
						\midiCtrl, {
							assets[\midiPort].control(assets[\midiChannel], assets[\midiCtrlNum], (val + morphVal).linlin(0, 1, 0, 127));
						},
						\midiNote, {
							// is anything needed here?
						}
					);
				}
			);
		};
	}

	updateMorphGroups {
		var parVal;
		[\morphA, \morphB, \morphC].do{|m|
			currentMorphVals[m].keysValuesDo{|par, val|
				parVal = currentParVals[par] ? 0;
				switch(type,
					\synth, {server.bind{parsimonia.moduleGroups[layer][row][col].set(par, val + parVal)}},

					\pattern, {server.bind{parsimonia.moduleGroups[layer][row][col].set(par, val + parVal)}},

					\vstInstr, {server.bind{parsimonia.moduleGroups[layer][row][col].set(par, val + parVal)}},

					\fx, {server.bind{module.set(par, val + parVal)}},

					\vstFx, { },

					\midi, { }
				);
			};
		};
	}

	update {|...args|
		switch(args[1],
			\buffer, {assets[\sampleBuffer] = args[2]},
			\reTrigFreq, {assets[\reTrigFreq] = args[2]}
		);
	}

	*synthFactory {|name, func|
		var noEnv = [\vsti]; // list synth modules that incoporate their own amp envelope here!
		if(noEnv.includes(name),
			{
				SynthDef(name, {
					|buf, freq, amp = 1, vol, gate = 1, outBus, fxBus, localFxBus,
					loop = 1, masterRel = 0.05, done = 2, outBusGain = 1, fxBusGain = 1, localFxBusGain = 1|
					var b = buf, f = freq, a = amp, g = gate, l = loop;
					var sig;

					sig = SynthDef.wrap(func, [\kr, \ar, \kr, \kr, \kr], [b, f, a, g, l]);

					sig = (sig * amp * vol.lag(0.1)).tanh;
					DetectSilence.ar(sig, doneAction: done);
					Out.ar(outBus, sig * outBusGain);
					Out.ar(fxBus, sig * fxBusGain);
					Out.ar(localFxBus, sig * localFxBusGain);
				}).add;
			}, {
				SynthDef(name, {
					|buf, freq, amp = 1, vol, gate = 1, outBus, fxBus, localFxBus,
					loop = 1, masterRel = 0.05, done = 2, outBusGain = 1, fxBusGain = 1, localFxBusGain = 1|
					var b = buf, f = freq, a = amp, g = gate, l = loop;
					var sig, env;
					env = EnvGen.kr(Env.asr(0.001, 1, masterRel), gate, doneAction: done);

					sig = SynthDef.wrap(func, [\kr, \ar, \kr, \kr, \kr], [b, f, a, g, l]);

					sig = (sig * env * amp * vol.lag(0.1)).tanh;
					Out.ar(outBus, sig * outBusGain);
					Out.ar(fxBus, sig * fxBusGain);
					Out.ar(localFxBus, sig * localFxBusGain);
				}).add;
			}
		);
	}

	*fxFactory {|name, func|
		SynthDef(name, {|inBus, outBus, amp = 1, vol, gate = 1, freq, freeDelay = 1, volControlsInput = 0, outBusGain = 1|
			var sig, env, input, f = freq, g = gate;
			env = EnvGen.kr(Env.asr(0.01, 1, 0.05), gate);
			input = LeakDC.ar(Select.ar(volControlsInput, [In.ar(inBus, 2), In.ar(inBus, 2) * vol.lag(0.1)]));

			sig = SynthDef.wrap(func, [\ar, \ar, \kr], [input, f, g]);

			FreeSelf.kr(DelayL.kr((gate-1).abs, 10, freeDelay));
			sig = Select.ar(volControlsInput, [(sig * env * amp.lag(0.2) * vol.lag(0.1)), (sig * amp.lag(0.2))]).tanh;
			Out.ar(outBus, sig * outBusGain.lag(0.1));
		}).add;
	}

	*build { }
}