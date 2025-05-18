ParsimoniaModule_AutoFilter : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \autoFilter;
	}

	*build {
		ParsimoniaModule.fxFactory(\autoFilter, {|input, freq, gate,
			lfoRate = 3.6, amount = 180|
			var sig, lfo, fm, in;
			lfo = LFNoise1.kr(lfoRate, amount);
			sig = BPF.ar(input, (900 + TRand.kr(-90, 90, gate) + lfo).clip(180, 9000), 0.09);
			sig = Pan2.ar((sig * 8).tanh, LFNoise1.kr(0.9, 0.5));
		});
	}
}