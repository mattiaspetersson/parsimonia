ParsimoniaModule_Saturation : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \saturation;
	}

	*build {
		ParsimoniaModule.fxFactory(\saturation, {|input, freq, gate,
			saturation = 9|
			var sig, satVal;
			satVal = saturation.lag(0.1).clip(1.0, 99.0);
			sig = (input * satVal).tanh / (satVal ** 0.6); // formula by James Harkins (satVal can't be 0!)
		});
	}
}