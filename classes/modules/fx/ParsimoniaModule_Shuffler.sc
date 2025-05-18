ParsimoniaModule_Shuffler : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \shuffler;
		assets[\shufBufL] = Buffer.alloc(server, server.sampleRate);
		assets[\shufBufR] = Buffer.alloc(server, server.sampleRate);
		additionalArgsArray = [\shufBufL, assets[\shufBufL], \shufBufR, assets[\shufBufR]];
	}

	*build {
		ParsimoniaModule.fxFactory(\shuffler, {|input, freq, gate,
			density = 18, shufBufL, shufBufR|
			var sigL, sigR, trig, bufL, bufR;
			bufL = shufBufL;
			bufR = shufBufR;
			RecordBuf.ar(input[0], bufL);
			RecordBuf.ar(input[1], bufR);
			trig = Dust.kr(density!2);
			sigL = TGrains.ar(
				2,
				trig[0],
				bufL,
				TChoose.kr(trig[0], [-1.01, -1, 1, 1.01]),
				0,
				TRand.kr(0.18, 0.81, trig[0]),
				TRand.kr(-1, 1, trig[0]),
				0.3,
				//TRand.kr(0.01, 0.07, trig[0]),
				//TRand.kr(0.01, 0.07, trig[0])
			);
			sigR = TGrains.ar(
				2,
				trig[1],
				bufR,
				TChoose.kr(trig[1], [-1.01, -1, 1, 1.01]),
				0,
				TRand.kr(0.18, 0.81, trig[1]),
				TRand.kr(-1, 1, trig[1]),
				0.3,
			);
			sigL+sigR;
		});
	}
}