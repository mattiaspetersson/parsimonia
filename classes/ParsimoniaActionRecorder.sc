ParsimoniaActionRecorder { // release version
	var parsimonia;
	var <recording, playing, player;
	var <actions, <deltas, time, globalActions;
	var currentLayer;

	*new {|aParsimonia|
		^super.newCopyArgs(aParsimonia).init;
	}

	init {
		parsimonia.addDependant(this);
		globalActions = [\djFilter_, \morphA_, \morphB_, \morphC_, \saturation_];
		recording = false;
		playing = false;
		currentLayer = parsimonia.manta.currentLayer;
	}

	record {|bool|
		recording = bool;
		if(bool, {
			if(playing, {this.play(false)});
			actions = [];
			deltas = [];
			time = nil; //latch rec start on first action
		}, {
			deltas = deltas.add(Main.elapsedTime - time);
		});
	}

	play {|bool|
		playing = bool;
		if(bool, {
			if(recording, {this.record(false)});
			player = {
				var d = deltas.copyRange(0, deltas.size-2);
				loop{
					d.do{|t, i|
						if(globalActions.includes(actions[i][0]).not, {
							if(parsimonia.manta.padStates[currentLayer][actions[i][1]][actions[i][2]].not, {
								parsimonia.performMsg(actions[i]);
							});
						}, {
							parsimonia.performMsg(actions[i]);
						});
						t.wait;
					};
					deltas.last.wait;
				};
			}.fork(SystemClock);
		}, {
			player.stop;
			// release hanging pads except for manually held ones
			parsimonia.manta.padStates[currentLayer].do{|row, r|
				row.do{|col, c|
					if(col.not, {
						parsimonia.playModule(currentLayer, r, c, false);
					});
				};
			};
		});
	}

	update {|...args|
		var a = args;
		a.removeAt(0);
		if(recording, {
			if(playing, {this.play(false)});
			deltas = deltas.add(Main.elapsedTime - (time ? Main.elapsedTime));
			actions = actions.add(args);
			time = Main.elapsedTime;
		});
	}
}