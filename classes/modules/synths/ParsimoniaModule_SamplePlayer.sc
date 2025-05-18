ParsimoniaModule_SamplePlayer : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \samplePlayer;
		if(keyAsset.notNil,
			{
				// first case: a path to a soundfile (or folder of soundfiles)
				case {keyAsset.isString} {
					if(keyAsset.isSoundFile, {
						this.loadSample(keyAsset);
						// TODO: possibility to load a folder to randomly play samples from
					}, {
						"No wav or aiff file found!".warn;
					});
				};

				// second case: an instance of an existing buffer
				case {keyAsset.class == Buffer} {
					this.pointToBuffer(keyAsset);
				};

				// third case: add this as a dependant to another module (usually a ParsimoniaSampleRecorder)
				case {keyAsset.isArray and: {keyAsset.isString.not}} {
					var m = parsimonia.modules[keyAsset[0]][keyAsset[1]][keyAsset[2]];
					if(m.notNil) {
						m.addDependant(this);
						//this.pointToBuffer(m.assets[\sampleBuffer]);
					} {
						"No module at this address".warn;
					};
				};
			}, {
				"Please provide a buffer, a valid path to a wav or aiff file or an array pointing to a sampler module!".warn
		});
	}

	*build {
		ParsimoniaModule.synthFactory(\samplePlayer_stereo, {|buf, freq, amp, gate, loop,
			reverse, reTrigFreq, speed = 1|
			var sig, r = (freq.cpsmidi - 48).midiratio * BufRateScale.kr(buf);
			sig = PlayBuf.ar(
				2,
				buf,
				Select.kr(reverse, [r, r * -1]),
				Impulse.ar((reTrigFreq * BufRateScale.kr(buf) * r) * speed),
				startPos: (BufFrames.kr(buf)-2) * reverse,
				loop: loop
			);
		});

		ParsimoniaModule.synthFactory(\samplePlayer_mono, {|buf, freq, amp, gate, loop,
			reverse, reTrigFreq, speed = 1|
			var sig, r = (freq.cpsmidi - 48).midiratio * BufRateScale.kr(buf);
			sig = PlayBuf.ar(
				1,
				buf,
				Select.kr(reverse, [r, r * -1]),
				Impulse.ar((reTrigFreq * BufRateScale.kr(buf) * r) * speed),
				startPos: (BufFrames.kr(buf)-2) * reverse,
				loop: loop
			);
			sig!2;
		});
	}
}

ParsimoniaModule_SamplePlayerCF : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \samplePlayerCF;
		if(keyAsset.notNil,
			{
				// first case: a path to a soundfile (or folder of such)
				case {keyAsset.isString} {
					if(keyAsset.isSoundFile, {
						this.loadSample(keyAsset);
						// TODO: possibility to load a folder to randomly play samples from
					}, {
						"No wav or aiff file found!".warn;
					});
				};

				// second case: an instance of an existing buffer
				case {keyAsset.class == Buffer} {
					this.pointToBuffer(keyAsset);
				};

				// third case: add this as a dependant to another module (usually a ParsimoniaSampleRecorder)
				case {keyAsset.isArray and: {keyAsset.isString.not}} {
					var m = parsimonia.modules[keyAsset[0]][keyAsset[1]][keyAsset[2]];
					if(m.notNil) {
						m.addDependant(this);
						//this.pointToBuffer(m.assets[\sampleBuffer]);
					} {
						"No module at this address".warn;
					};
				};
			}, {
				"Please provide a buffer, a valid path to a wav or aiff file or an array pointing to a sampler module!".warn
		});
	}

	*build {
		ParsimoniaModule.synthFactory(\samplePlayerCF_stereo, {|buf, freq, amp, gate, loop,
			reverse, loopPointXfade = 0.1|
			var sig, loopTrig, r = (freq.cpsmidi - 48).midiratio * BufRateScale.kr(buf);
			loopTrig = Impulse.ar(SampleRate.ir/(BufFrames.kr(buf)-(BufFrames.kr(buf)*loopPointXfade)));
			sig = PlayBufCF.ar(
				2,
				buf,
				Select.kr(reverse, [r, r * -1]),
				loopTrig,
				startPos: (BufFrames.kr(buf)-2) * reverse,
				loop: loop
			);
		});

		ParsimoniaModule.synthFactory(\samplePlayerCF_mono, {|buf, freq, amp, gate, loop,
			reverse, loopPointXfade = 0.1|
			var sig, loopTrig, r = (freq.cpsmidi - 48).midiratio * BufRateScale.kr(buf);
			loopTrig = Impulse.ar(SampleRate.ir/(BufFrames.kr(buf)-(BufFrames.kr(buf)*loopPointXfade)));
			sig = PlayBufCF.ar(
				1,
				buf,
				Select.kr(reverse, [r, r * -1]),
				loopTrig,
				startPos: (BufFrames.kr(buf)-2) * reverse,
				loop: loop
			);
			sig!2;
		});
	}
}

ParsimoniaModule_SamplePlayerWarp : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \samplePlayerWarp;
		if(keyAsset.notNil,
			{
				// first case: a path to a soundfile (or folder of such)
				case {keyAsset.isString} {
					if(keyAsset.isSoundFile, {
						this.loadSample(keyAsset);
						// TODO: possibility to load a folder to randomly play samples from
					}, {
						"No wav or aiff file found!".warn;
					});
				};

				// second case: an instance of an existing buffer
				case {keyAsset.class == Buffer} {
					this.pointToBuffer(keyAsset);
				};

				// third case: add this as a dependant to another module (usually a ParsimoniaSampleRecorder)
				case {keyAsset.isArray and: {keyAsset.isString.not}} {
					var m = parsimonia.modules[keyAsset[0]][keyAsset[1]][keyAsset[2]];
					if(m.notNil) {
						m.addDependant(this);
						//this.pointToBuffer(m.assets[\sampleBuffer]);
					} {
						"No module at this address".warn;
					};
				};
			}, {
				"Please provide a buffer, a valid path to a wav or aiff file or an array pointing to a sampler module!".warn
		});
	}

	*build {
		ParsimoniaModule.synthFactory(\samplePlayerWarp_stereo, {|buf, freq, amp, gate, loop,
			reverse, reTrigFreq, speed = 1, size = 0.2, randomness = 0.2|
			var sig, r = (freq.cpsmidi - 48).midiratio * BufRateScale.kr(buf), pointer;
			pointer = Phasor.ar(
				Impulse.ar((reTrigFreq * BufRateScale.kr(buf) * r) * speed),
				speed * r,
				0,
				BufFrames.kr(buf)
			) / BufFrames.kr(buf);
			sig = Warp1.ar(
				2,
				buf,
				pointer,
				Select.kr(reverse, [r, r * -1]),
				size,
				windowRandRatio: randomness
			);
		});

		ParsimoniaModule.synthFactory(\samplePlayerWarp_mono, {|buf, freq, amp, gate, loop,
			reverse, reTrigFreq, speed = 1, size = 0.2, randomness = 0.2|
			var sig, r = (freq.cpsmidi - 48).midiratio * BufRateScale.kr(buf), pointer;
			pointer = Phasor.ar(
				Impulse.ar((reTrigFreq * BufRateScale.kr(buf) * r) * speed),
				speed * r,
				0,
				BufFrames.kr(buf)
			) / BufFrames.kr(buf);
			sig = Warp1.ar(
				1,
				buf,
				pointer,
				Select.kr(reverse, [r, r * -1]),
				size,
				windowRandRatio: randomness
			);
			sig!2;
		});
	}
}