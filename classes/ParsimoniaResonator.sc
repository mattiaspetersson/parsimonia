ParsimoniaResonator { // release version
	var server, recInBusLayer1, recInBusLayer2, recInBusLayer3, resInBus, outBus, group, level;
	var buffer, recSynth, playSynth;

	*new{|server, recInBusLayer1, recInBusLayer2, recInBusLayer3, resInBus, outBus, group, level|
		^super.newCopyArgs(
			server,
			recInBusLayer1,
			recInBusLayer2,
			recInBusLayer3,
			resInBus,
			outBus,
			group,
			level
		).initParsimoniaResonator;
	}

	initParsimoniaResonator{
		fork{
			buffer = Buffer.alloc(server, server.sampleRate, 2);
			server.sync;
			recSynth = Synth(\parsimoniaResonanceRecorder, [
				\buf, buffer,
				\inBus1, recInBusLayer1,
				\inBus2, recInBusLayer2,
				\inBus3, recInBusLayer3
			],group);
			playSynth = Synth(\parsimoniaResonancePlayback, [
				\buf, buffer,
				\inBus, resInBus,
				\outBus, outBus,
				\level, level.asMap
			], group);
		};
	}

	playResonance {|bool|
		if(bool, {
			playSynth.set(\rate, 1);
		}, {
			playSynth.set(\rate, 1);
		});
	}

	*synthDefs {
		SynthDef(\parsimoniaResonanceRecorder, {|buf, rec = 1, inBus1, inBus2, inBus3|
			var input1, input2, input3;
			input1 = In.ar(inBus1, 2);
			input2 = In.ar(inBus2, 2);
			input3 = In.ar(inBus3, 2);
			RecordBuf.ar(input1 + input2 + input3, buf, preLevel: 0.9, run: rec);
		}).add;

		SynthDef(\parsimoniaResonancePlayback, {|buf, rate = 1, inBus, outBus, type = 0.7, amp = 1, level|
			var sig, input;
			input = In.ar(inBus, 2);
			sig = PlayBuf.ar(2, buf, rate, loop: 1);
			sig = Convolution.ar(NHHall.ar(HPF.ar(input, 120), 0.18, 0.7) * 0.8, HPF.ar(sig, 120), 1024);
			sig = sig * amp * level.lag(0.1);
			Out.ar(outBus, Limiter.ar(sig));
		}).add;
	}
}