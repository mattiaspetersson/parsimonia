ParsimoniaModule_MIDINote : ParsimoniaModule {
	initModule {
		type = \midi;
		name = \midiNote;
		if(keyAsset.isArray, { // should be [device, port, channel, noteNum]
			//assets[\midiPort] = MIDIOut.newByNameLinux(keyAsset[0], keyAsset[1]).latency_(server.latency);
			assets[\midiPort] = MIDIOut.newByName(keyAsset[0], keyAsset[1]).latency_(server.latency);
			assets[\midiChannel] = keyAsset[2];
			assets[\midiNoteNum] = keyAsset[3] ? 0; // ? padNum if posAsDegre == true
		});
	}
}