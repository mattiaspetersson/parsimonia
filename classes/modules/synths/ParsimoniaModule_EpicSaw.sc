ParsimoniaModule_EpicSaw : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \epicSaw;
	}

	*build {
		ParsimoniaModule.synthFactory(\epicSaw, {|buf, freq, amp, gate, loop,
			atk = 0.09, dcy = 1.8|
			var sig, env;
			env = EnvGen.kr(Env.asr(atk, 1, dcy), gate, doneAction: 2);
			sig = RLPF.ar(
				Saw.ar([freq, freq + TRand.kr(0.0, 0.09, gate)]),
				XLine.kr(900, 9000, 36) + TRand.kr(9, 90, gate),
				0.4 + LFNoise1.kr(0.09, 0.2),
				env
			).tanh + SinOsc.ar(freq/2, 0, env);
			sig;
		});
	}
}
