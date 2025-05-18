ParsimoniaModule_HWinputStereo : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \hwInputStereo;
		if(keyAsset.isArray, {this.setInput(keyAsset)}, {this.setInput([0, 1])});
		additionalArgsArray = [\inBus, assets[\inBus], \gain, assets[\gain]];
	}

	*build {
		ParsimoniaModule.synthFactory(\hwInputStereo, {|buf, freq, amp, gate, loop,
			inBus = 0, gain = 1|
			SoundIn.ar([inBus, inBus+1], gain);
		});
	}
}

ParsimoniaModule_HWinputMono : ParsimoniaModule {
	initModule {
		type = \synth;
		name = \hwInputMono;
		if(keyAsset.isArray, {this.setInput(keyAsset)}, {this.setInput([0, 1])});
		additionalArgsArray = [\inBus, assets[\inBus], \gain, assets[\gain]];
	}

	*build {
		ParsimoniaModule.synthFactory(\hwInputMono, {|buf, freq, amp, gate, loop,
			inBus = 0, gain = 1, hpfFreq = 30|
			HPF.ar(SoundIn.ar([inBus, inBus], gain), hpfFreq);
		});
	}
}