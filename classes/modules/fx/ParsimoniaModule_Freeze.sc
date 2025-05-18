ParsimoniaModule_Freeze : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \freeze;
		additionalArgsArray = [
			\freeDelay, 0,
		];
	}

	*build {
		ParsimoniaModule.fxFactory(\freeze, {|input, freq, gate|
			var chainL, chainR, sig;
			chainL = FFT(LocalBuf(2048), input[0]);
			chainL = PV_Freeze(chainL, 1);
			chainR = FFT(LocalBuf(2048), input[1]);
			chainR = PV_Freeze(chainR, 1);
			sig = [IFFT(chainL), IFFT(chainR)];
		});
	}
}