ParsimoniaModule_MIDICtrl : ParsimoniaModule {
	initModule {
		type = \midi;
		name = \midiCtrl;
		if(keyAsset.isArray, { // should be [device, port, channel, ctrlNum, ctrlVal]
			assets[\midiPort] = MIDIOut.newByName(keyAsset[0], keyAsset[1]).latency_(server.latency);
			assets[\midiChannel] = keyAsset[2];
			assets[\midiCtrlNum] = keyAsset[3];
			assets[\midiCtrlVal] = keyAsset[4]; // could be a function
		});
	}
}