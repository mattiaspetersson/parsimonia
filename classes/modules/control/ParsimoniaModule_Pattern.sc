ParsimoniaModule_Pattern : ParsimoniaModule {
	initModule {
		type = \pattern;
		name = \pattern;
		if(keyAsset.isKindOf(Pattern), {
			assets[\pattern] = Pchain(
				keyAsset,
				(
					\buf: assets[\sampleBuffer], // how is this used?
					\group: parsimonia.moduleGroups[row][col],
					\scale: scale,
					\mtranspose: if(posAsDegree, {
						col+(row*8);
					}, {
						0;
					}),
					\ctranspose: transpose ? 0,
					\outBus: parsimonia.masterBus,
					\fxBus: parsimonia.fxBus,
					\localFxBus: parsimonia.localFxBusses[row][col]
				),
				//additionalArgsArray.asPattern
			);
		});
	}
}