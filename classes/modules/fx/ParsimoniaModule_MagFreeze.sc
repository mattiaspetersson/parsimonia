ParsimoniaModule_MagFreeze : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \magFreeze;
	}

	*build {
		ParsimoniaModule.fxFactory(\magFreeze, {|input, freq, gate|
			var chainL, chainR, sig;
			chainL = FFT(LocalBuf(2048), input[0]);
			chainL = PV_MagFreeze(chainL, 1);
			chainR = FFT(LocalBuf(2048), input[1]);
			chainR = PV_MagFreeze(chainR, 1);
			sig = [IFFT(chainL), IFFT(chainR)];
		});
	}
}