ParsimoniaModule_PeakEQ : ParsimoniaModule {
	initModule {
		type = \fx;
		name = \peakEQ;
		if(keyAsset.isArray) {
			this.setModulePar(\eqFreq, keyAsset[0]);
			this.setModulePar(\rq, keyAsset[1]);
			this.setModulePar(\eqGain, keyAsset[2]);
			//assets[\eqFreq] = keyAsset[0];
			//assets[\rq] = keyAsset[1];
			//assets[\eqGain] = keyAsset[2];
		} {
			"PeakEQ: Key asset must be an array with freq, rq and gain".warn;
		};

		//additionalArgsArray = [\eqFreq, assets[\eqFreq], \rq, assets[\rq], \eqGain, assets[\eqGain]];
	}

	*build {
		ParsimoniaModule.fxFactory(\peakEQ, {|input, freq, gate,
			eqFreq, rq = 0.2, eqGain = 0|
			BPeakEQ.ar(input, eqFreq.clip(20, 20000), rq.clip(0.001, 0.99999), eqGain.clip(-60.dbamp, 60.dbamp));
		});
	}
}