ParsimoniaModule_Sine : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \sine;
	}

	*build {
		ParsimoniaModule.synthFactory(\sine, {|buf, freq, amp, gate, loop,
			fb|
			SinOscFB.ar(freq!2, fb, 0.1);
		});
	}
}