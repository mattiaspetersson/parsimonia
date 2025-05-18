ParsimoniaModule_Reverb : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \reverb;
		additionalArgsArray = [
			\freeDelay, 5,
			\volControlsInput, 1
		];
	}

	*build {
		ParsimoniaModule.fxFactory(\reverb, {|input, freq, gate,
			reverbTime = 1.8, hpfFreq = 60|
			NHHall.ar(HPF.ar(input, hpfFreq), reverbTime);
		});
	}
}