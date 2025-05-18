ParsimoniaModule_PinkNoise : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \pinkNoise;
	}

	*build {
		ParsimoniaModule.synthFactory(\pinkNoise, {|buf, freq, amp, gate, loop|
			PinkNoise.ar(0.2!2);
		});
	}
}