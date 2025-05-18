ParsimoniaModule_StringResonator : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \stringResonator;
	}

	*build {
		ParsimoniaModule.fxFactory(\stringResonator, {|input, freq, gate,
			decayTime = 1.8|
			var pluck;
			pluck = Pluck.ar(input, Impulse.ar(freq), 1, (freq.clip(20, 20000)).reciprocal, decayTime);
			pluck!2;
		});
	}
}