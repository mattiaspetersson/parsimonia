ParsimoniaModule_VolumeDip : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \volumeDip;
	}

	*build {
		ParsimoniaModule.fxFactory(\volumeDip, {|input, freq, gate,
			amount = 1|
			var sig;
			sig = (input * -1) * amount;
		});
	}
}