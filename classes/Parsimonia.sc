/*
An open system for the Snyderphonics Manta where one can map one or more modules to each pad.
Modules can be a looped or oneshot sample, a synth, an effect (can also be a vst plug-in) or a MIDI or OSC controller.
Held pads can be ”freezed” by pressing a pedal (like the sostenuto pedal on a grand piano).
The area covered on each pad controls the volume for the respective modules.
*/

Parsimonia { // release version
	var <resonator, pathToRAVEmodels;
	var <server, <masterSynth, <masterBus, <fxBus, <tempoClock, <tempo, tempoNotifications;
	var <modules, <modulesGroup, <moduleGroups, <fxGroup, <localFxGroups, <localFxBusses;
	var <scale, <tuning, <manta, <actionRecorder, isPlaying;
	var <vol, <saturation, <djFilter, <resonance, <morphA, <morphB, <morphC;
	var <beatDurBus, <functionState;
	var globalAssets, mantaOscCondition;
	var resonanceGroup, resonators, resonanceLevelBus;

	*new {|resonator = \off, pathToRAVEmodels = "/Users/mattpete/Dropbox/work/RAVEmodels/"|
		^super.newCopyArgs(resonator, pathToRAVEmodels).init;
	}

	init {
		ParsimoniaMantaOSC.startMantaOSC;
		//Server.supernova;
		Server.scsynth;
		server = Server.default;
		server.options.hardwareBufferSize = 128;
		server.options.memSize_(262144);
		server.latency = 0.01;

		//tempoClock = TempoClock.default;
		tempoClock = LinkClock().latency_(server.latency);
		tempoNotifications = SimpleController(tempoClock)
		.put(\tempo, {
			defer {"tempo changed to % bpm".format(tempoClock.tempo * 60).postln};
		})
		.put(\numPeers, {
			defer {"number of link peers is now %".format(tempoClock.numPeers).postln};
		});

		modules = Array.fill3D(3, 6, 8, {()});
		isPlaying = Array.fill3D(3, 6, 8, false);
		tuning = \et12;
		globalAssets = ();
		this.functionState_(0);
		this.scale_(\harmonicMinor); // default scale
		this.tuning_(\et12); // default tuning

		server.waitForBoot{
			masterBus = Bus.audio(server, 2);
			fxBus = Bus.audio(server, 2);
			localFxBusses = Array.fill3D(3, 6, 8, {Bus.audio(server, 2)});
			modulesGroup = ParGroup(server);
			moduleGroups = Array.fill3D(3, 6, 8, {ParGroup(modulesGroup)});
			localFxGroups = Array.fill3D(3, 6, 8, {|l, r, c| ParGroup.after(moduleGroups[l][r][c])});
			resonanceGroup = ParGroup.after(modulesGroup);
			fxGroup = ParGroup.after(modulesGroup);
			beatDurBus = Bus.control(server, 1);
			resonanceLevelBus = Bus.control(server, 1);

			SynthDef(\master, {|input, output, vol, hpfFreq = 20, lpfFreq = 20000, saturation = 1|
				var in, sig, satVal;
				in = In.ar(input, 2);
				sig = RLPF.ar(
					RHPF.ar(in,
						hpfFreq.lag(0.1).clip(20, 20000),
						0.7,
					),
					lpfFreq.lag(0.1).clip(20, 20000),
					0.7,
				);
				//sig = in;
				satVal = saturation.lag(0.1).clip(1.0, 99.0);
				sig = (sig * satVal).tanh / (satVal ** 0.6); // formula by James Harkins (satVal can't be 0!)
				sig = BLowShelf.ar(sig, 200, 0.5, satVal.linlin(1.0, 99.0, 0.0, 9.0)); // boost bass when saturating
				sig = Limiter.ar(sig);
				Out.ar(output, sig * vol.lag(0.1));
			}).add;

			ParsimoniaResonator.synthDefs;
			ParsimoniaModule.allSubclasses.do{|class| class.build};

			server.sync;

			if(resonator != \off) {
				var n;
				n = PathName(pathToRAVEmodels).files.collect{|f|
					f.fileName;
				};

				// load convolution resonator
				case {resonator == \convolution} {

					server.sync;
					resonators = Array.fill2D(6, 8, {|r, c| // 2D, since it's enough with one layer of convolution resonators
						ParsimoniaResonator(
							server,
							localFxBusses[0][r][c].index,
							localFxBusses[1][r][c].index,
							localFxBusses[2][r][c].index,
							fxBus,
							masterBus,
							fxGroup,
							resonanceLevelBus
						)
					});
				};

				// load a RAVE model
				case {n.includesEqual((resonator++".ts"))} {
					NN.load(resonator, pathToRAVEmodels++resonator++".ts");
					server.sync;
					SynthDef(\nn, {|inBus, outBus, amp = 1, level|
						var sig, input;
						input = In.ar(inBus, 2);
						sig = NN(resonator, \forward).ar(input * 8, 4096); // look for ways to lower this latency!
						sig = sig * amp * level.lag(0.1);
						Out.ar(outBus, Limiter.ar(sig));
					}).add;
					server.sync;
					resonators = Synth(\nn, [
						\inBus, fxBus,
						\outBus, masterBus,
						\level, resonanceLevelBus.asMap
					], fxGroup);
				};
			};

			server.sync;

			masterSynth = Synth.after(fxGroup, \master, [\input, masterBus, \output, 0]);
			server.sync;
			1.wait;

			manta = ParsimoniaMantaOSC(this);
			//manta = ParsimoniaManta(this);
			actionRecorder = ParsimoniaActionRecorder(this);

			// set default values
			this.djFilter_(0.5);
			this.saturation_(0);
			this.vol_(1);
			this.morphA_(0);
			this.morphB_(0);
			this.morphC_(0);
			this.tempo_(126);
			this.resonanceLevel_(0);
		};
	}

	addModule {|layer, row, col, key, module, keyAsset, posAsDegree = false, transpose = 0, localFx = false, resumePlayback = false, quant = 0|
		// a keyAsset is a path to a file, a ControlSpec, a pattern or a plugin name or path
		module = ('ParsimoniaModule_'++module).asSymbol.asClass;
		if(modules[layer][row][col][key].isNil, {
			modules[layer][row][col][key] = module.new(
				layer,
				row,
				col,
				key,
				this,
				keyAsset,
				posAsDegree,
				transpose,
				localFx,
				resumePlayback,
				quant
			);
		}, {
			this.removeModule(layer, row, col, key);
			modules[layer][row][col][key] = module.new(
				layer,
				row,
				col,
				key,
				this,
				keyAsset,
				posAsDegree,
				transpose,
				localFx,
				resumePlayback,
				quant
			);
		});
	}

	removeModule {|layer, row, col, key|
		if(modules[layer][row][col][key].notNil, {
			modules[layer][row][col][key].free;
			modules[layer][row][col][key] = nil;
		});
	}

	removeAllModulesAt {|layer, row, col|
		if(modules[layer][row][col].notNil, {
			modules[layer][row][col].keysValuesDo{|key, val|
				val.free;
				modules[layer][row][col][key] = nil;
			};
		});
	}

	removeAllModules {
		48.do{|i|
			this.removeAllModulesAt((i/8).asInteger, i%8);
		};
	}

	show {|layer, row, col|
		modules[layer][row][col].postln;
	}

	getControlNames {|layer, row, col|
		modules[layer][row][col].keysValuesDo{|key, val|
			if(val.isKindOf(ParsimoniaModule_Set).not, {
				"% parameters: %".format(key, SynthDescLib.global[val.name].controlNames).postln;
			});
		};
	}

	playModule {|layer, row, col, bool, velocity|
		modules[layer][row][col].keysValuesDo{|key, val| val.play(bool, velocity)};
		if(resonator != \off) {
			case {resonator == \convolution} {
				resonators.do{|row|
					row.do{|col| col.playResonance(bool)};
				};
			};
		};
		this.changed(\playModule, layer, row, col, bool, velocity);
	}

	setModulePar {|layer, row, col, par, val, key|
		if(key.isNil) {
			modules[layer][row][col].keysValuesDo{|k, module|
				module.setModulePar(par, val);
			};
		} {
			modules[layer][row][col][key].postln;
			modules[layer][row][col][key].setModulePar(par, val);
		};
		this.changed(\setModulePar, layer, row, col, par, val, key);
	}

	vol_{|val|
		vol = val;
		server.bind{masterSynth.set(\vol, val)};
		this.changed(\vol, val);
	}

	scale_{|scaleNameOrArray|
		if(scaleNameOrArray.isArray.not, {
			scale = Scale.perform(scaleNameOrArray);
			scale.tuning = tuning;
		}, {
			scale = Scale.new(scaleNameOrArray, 12);
			scale.tuning = tuning;
		});
	}

	tuning_{|tuningName|
		tuning = Tuning.perform(tuningName);
		scale.tuning = tuning;
	}

	tempo_{|bpm|
		tempo = bpm;
		tempoClock.tempo = bpm/60;
		beatDurBus.set(tempoClock.beatDur);
	}

	djFilter_{|val| // 0-1
		djFilter = val;
		server.bind{
			masterSynth.set(\lpfFreq, val.linexp(0, 0.5, 20, 20000));
			masterSynth.set(\hpfFreq, val.linexp(0.5, 1, 20, 20000));
		};
		this.changed(\djFilter_, val);
	}

	saturation_{|val|
		saturation = val;
		server.bind{masterSynth.set(\saturation, val.linlin(0, 1, 1, 99))};
		this.changed(\saturation_, val);
	}

	resonanceLevel_{|val|
		resonance = val;
		resonanceLevelBus.set(val);
		this.changed(\resonance_, val);
	}

	morphA_{|val, midiCtrl|
		morphA = val;
		this.setMorph(\morphA, val);
		this.changed(\morphA_, val);
	}

	morphB_{|val|
		morphB = val;
		this.setMorph(\morphB, val);
		this.changed(\morphB_, val);
	}

	morphC_{|val|
		morphC = val;
		this.setMorph(\morphC, val);
		this.changed(\morphC_, val);
	}

	setMorph {|morphGroup, val|
		modules.flat.do{|dict|
			dict.keysValuesDo{|k, v|
				v.setMorph(morphGroup, val);
			};
		};
	}

	functionState_{|state|
		functionState = state;
		this.changed(\functionState, state);
	}

	mantaPadToggle {|layer, col, row, bool|
		manta.padToggles[layer][col][row] = bool;
	}

	mantaPadColorInvert {|layer, col, row, bool|
		manta.padColorInvert[layer][col][row] = bool;
		this.changed(\playModule, layer, col, row, false, 0);
	}
}