ParsimoniaModule_Repeater : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \repeater;
		assets[\repeaterGroup] = Group(parsimonia.fxGroup);
		assets[\repeaterBuf] = Buffer.alloc(server, server.sampleRate * 4, 2);
		assets[\phasorBus] = Bus.audio(server, 1);
		assets[\repeaterRecorderSynth] = Synth(\repeaterRecorder, [
			\input, parsimonia.fxBus,
			\buf, assets[\repeaterBuf],
			\phasorOut, assets[\phasorBus]
		], assets[\repeaterGroup]);
		additionalArgsArray = [
			\repeaterBuf, assets[\repeaterBuf],
			\phasorIn, assets[\phasorBus]
		];
	}

	*build {
		SynthDef(\repeaterRecorder, {|buf, rec = 1, input, phasorOut, t_trig = 1|
			var phasor;
			phasor = Phasor.ar(t_trig, BufRateScale.kr(buf), 0, BufFrames.kr(buf));
			BufWr.ar(In.ar(input, 2), buf, phasor * rec);
			Out.ar(phasorOut, phasor);
		}).add;

		ParsimoniaModule.fxFactory(\repeater, {|input, freq, gate,
			t_trig = 0, beatDurInSecs, repeaterBuf, repeats = 1, phasorIn|
			var phasor, rec, play, trigPos, repeatValue, f, repeatStartVal, antiClickEnv;
			f = BufFrames.kr(repeaterBuf);
			//beatDurInSecs.poll;
			phasor = In.ar(phasorIn, 1);
			repeatValue = (beatDurInSecs / repeats) * BufSampleRate.kr(repeaterBuf);
			trigPos = Latch.ar(phasor - 1, gate); // phasors pos when triggered
			repeatStartVal = ((trigPos - repeatValue) + f) % f;
			antiClickEnv = IEnvGen.ar(
				Env([0, 1, 0], [1, 1], [-9, 9]),
				Phasor.ar(0, (beatDurInSecs / repeats))
			);
			play = BufRd.ar(
				2,
				repeaterBuf,
				Phasor.ar(
					0,
					BufRateScale.kr(repeaterBuf),
					Select.ar(trigPos > repeatStartVal, [trigPos, repeatStartVal]),
					Select.ar(trigPos < repeatStartVal, [trigPos, repeatStartVal])
				)
			);
			//play * antiClickEnv; //glitches
		});
	}
}