ParsimoniaModule_SampleRecorderMono : ParsimoniaModule {
	var recordedTime;

	initModule {
		type = \synth;
		name = \sampleRecorderMono;
		assets[\inBus] = keyAsset ? server.options.numOutputBusChannels; // set input bus index or else use the first input on soundcard
		assets[\sampleBuffer] = Buffer.alloc(server, server.sampleRate * 60, 1);
		additionalArgsArray = [\inBus, assets[\inBus], \gain, assets[\gain]];
	}

	recTimer {|bool|
		if(bool) {
			recordedTime = Main.elapsedTime
		} {
			recordedTime = Main.elapsedTime - recordedTime;
			assets[\reTrigFreq] = recordedTime.reciprocal;
			this.changed(\buffer, assets[\sampleBuffer]);
			this.changed(\reTrigFreq, assets[\reTrigFreq]);
		};
	}

	*build {
		ParsimoniaModule.synthFactory(\sampleRecorderMono, {|buf, freq, amp, gate, loop,
			inBus, reTrigFreq, overdub = 0|
			var input = SoundIn.ar(inBus);
			RecordBuf.ar(
				input,
				buf,
				preLevel: overdub
			);
			0; // do not send audio through while recording
		});
	}
}

ParsimoniaModule_SampleRecorderStereo : ParsimoniaModule {
	var recordedTime;

	initModule {
		type = \synth;
		name = \sampleRecorderStereo;
		assets[\inBus] = keyAsset ? server.options.numOutputBusChannels; // set input bus index or else use the first input on soundcard
		assets[\sampleBuffer] = Buffer.alloc(server, server.sampleRate * 60, 2);
		additionalArgsArray = [\inBus, assets[\inBus], \gain, assets[\gain]];
	}

	recTimer {|bool|
		if(bool) {
			recordedTime = Main.elapsedTime
		} {
			recordedTime = Main.elapsedTime - recordedTime;
			assets[\reTrigFreq] = recordedTime.reciprocal;
			this.changed(\buffer, assets[\sampleBuffer]);
			this.changed(\reTrigFreq, assets[\reTrigFreq]);
		};
	}

	*build {
		ParsimoniaModule.synthFactory(\sampleRecorderStereo, {|buf, freq, amp, gate, loop,
			inBus, reTrigFreq, overdub = 0|
			var input = SoundIn.ar([inBus, inBus + 1]);
			RecordBuf.ar(
				input,
				buf,
				preLevel: overdub
			);
			0; // do not send audio through while recording
		});
	}
}