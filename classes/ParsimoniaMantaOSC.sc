ParsimoniaMantaOSC { // release version
	var parsimonia, host, port, recvPort;
	var <manta;
	var <currentLayer, <padStates, <padToggles, <padColorInvert, padValues, padSostenuto;
	var buttonStates, buttonValues, buttonSostenuto, buttonTimers, buttonTimeOuts, previousButtonState;
	var sliderValues, pedal;
	var functionModeFlashFork;
	var respondsToMidi, padsMidiNoteOn, padsMidiNoteOff;
	classvar pads, buttons, sliders, <mantaOscPid;

	*new {|aParsimonia, host = "127.0.0.1", port = 31417, recvPort = 31416|
		^super.newCopyArgs(aParsimonia, host, port, recvPort).init;
	}

	*startMantaOSC {
		Platform.case(
			\linux, {
				var pid = "pidof MantaOSC".unixCmd;
				("kill -9 "++pid).unixCmd;
				mantaOscPid = "/opt/MantaOSC".unixCmd
			},
			\osx, {
				var pid = "pgrep -n MantaOSC".unixCmd;
				("kill -9 "++pid).unixCmd;
				//mantaOscPid = "/usr/local/bin/MantaOSC".unixCmd;
				mantaOscPid = "/Applications/MantaOSC".unixCmd;
			}
		);
	}

	init {
		parsimonia.addDependant(this);
		manta = NetAddr(host, port);
		manta.sendMsg('/manta/ledcontrol', 'padandbutton', 1); // enable led control for pads and buttons
		manta.sendMsg('/manta/ledcontrol', 'slider', 1); // enable led control for sliders
		48.do{|i| manta.sendMsg('/manta/led/pad', 'off', i)}; // turn off hanging leds
		4.do{|i| manta.sendMsg('/manta/led/button', 'off', i)};
		MIDIClient.init;
		MIDIIn.connectAll;
		padStates = Array.fill3D(4, 6, 8, false);
		padToggles = Array.fill3D(4, 6, 8, false);
		padColorInvert = Array.fill3D(4, 6, 8, false);
		padSostenuto = Array.fill3D(4, 6, 8, false);
		respondsToMidi = Array.fill3D(4, 6, 8, false);
		padValues = Array.fill3D(4, 6, 8, 0);
		buttonStates = 0!4;
		buttonTimers = nil!4;
		buttonTimeOuts = false!4;
		sliderValues = 0!2;
		currentLayer = 0;
		[pads, buttons, sliders].do{|f| if(f.notNil, {f.free})}; // frees the OSCFuncs if any

		pads = OSCFunc({|msg|
			var l = currentLayer, r = (msg[1] / 8).asInteger, c = msg[1] % 8, v = msg[2];
			if(padToggles[l][r][c].not, {
				if(padSostenuto[l][r][c].not, {
					parsimonia.playModule(l, r, c, v.asBoolean, v); //send both boolean and actual value for midi velocity!
					padStates[l][r][c] = v.asBoolean;
				});
			}, {
				if(v.asBoolean, {
					padStates[l][r][c] = padStates[l][r][c].not;
					parsimonia.playModule(l, r, c, padStates[l][r][c], v); //send both boolean and actual value for midi velocity!
				});
			});
		}, '/manta/velocity/pad', recvPort: recvPort);

		padValues = OSCFunc({|msg|
			var l = currentLayer, r = (msg[1] / 8).asInteger, c = msg[1] % 8, v = msg[2];
			if(padToggles[l][r][c].not, {
				if(padSostenuto[l][r][c].not, {
					// this might need to be a control bus for patterns to work?
					parsimonia.setModulePar(l, r, c, \vol, v.lincurve(20, 200, 0, 1, 3));
				});
			}, {
				parsimonia.setModulePar(l, r, c, \vol, padStates[l][r][c].asInteger);
			});
		}, '/manta/continuous/pad', recvPort: recvPort);

		buttons = OSCFunc({|msg|
			var i = msg[1], v = msg[2];
			switch(i,
				0, { // vol, filter, saturation.
					if(v != 0, {
						buttonStates[i] = (buttonStates[i]+1)%3;
						manta.sendMsg('/manta/led/button', buttonStates[i].asMantaLedColor, i);
						switch(buttonStates[0],
							0, {
								8.do{|i|
									manta.sendMsg(
										'/manta/led/slider',
										((parsimonia.vol*8).round>i).asInteger.asMantaLedColor,
										0,
										128>>i
									);
								};
							},
							1, {
								8.do{|i|
									manta.sendMsg(
										'/manta/led/slider',
										((parsimonia.djFilter*8).round>i).asInteger.asMantaLedColor,
										0,
										128>>i
									);
								};
							},
							2, {
								8.do{|i|
									manta.sendMsg(
										'/manta/led/slider',
										((parsimonia.saturation*8).round>i).asInteger.asMantaLedColor,
										0,
										128>>i
									);
								};
							},
						);
					});
				},

				1, { // layer button. toggle between layers 0 - 2. long press --> state 3 (Utility page).
					if(v != 0) {
						if(buttonStates[i] == 3) {
							if(buttonTimers[i].notNil) {buttonTimers[i].cancel};
							buttonStates[i] = previousButtonState;
							currentLayer = previousButtonState;
							this.updatePads(currentLayer);
							functionModeFlashFork.stop;
							manta.sendMsg('/manta/led/button', buttonStates[i].asMantaLedColor, i);
						}{
							currentLayer = buttonStates[i];
							buttonTimeOuts[i] = false;
							buttonTimers[i] = AppClock.timer(2, false, {
								buttonTimeOuts[i] = true;
								previousButtonState = buttonStates[i];
								buttonStates[i] = 3;
								currentLayer = 3;
								this.updatePads(currentLayer);
								parsimonia.functionState_(buttonStates[i]);

								functionModeFlashFork = {
									{
										manta.sendMsg('/manta/led/button', 'red', 1);
										0.2.wait;
										manta.sendMsg('/manta/led/button', 'amber', 1);
										0.2.wait;
										manta.sendMsg('/manta/led/button', 'off', 1);
										0.2.wait;
									}.loop;
								}.fork(AppClock);
							});
						}
					} {
						if(buttonStates[i] != 3) {
							if(buttonTimers[i].notNil) {buttonTimers[i].cancel};
							if(buttonTimeOuts[i].not) {
								buttonStates[i] = (buttonStates[i]+1)%3;
								currentLayer = buttonStates[i];
								this. updatePads(currentLayer);
								manta.sendMsg('/manta/led/button', buttonStates[i].asMantaLedColor, i);
							}
						}
					}
				},

				2, { // morph groups
					if(v != 0, {
						buttonStates[i] = (buttonStates[i]+1)%3;
						manta.sendMsg('/manta/led/button', buttonStates[i].asMantaLedColor, i);
						switch(buttonStates[2],
							0, {
								8.do{|i|
									manta.sendMsg(
										'/manta/led/slider',
										((parsimonia.morphA*8).round>i).asInteger.asMantaLedColor,
										1,
										128>>i
									);
								};
							},
							1, {
								8.do{|i|
									manta.sendMsg(
										'/manta/led/slider',
										((parsimonia.morphB*8).round>i).asInteger.asMantaLedColor,
										1,
										128>>i
									);
								};
							},
							2, {
								8.do{|i|
									manta.sendMsg(
										'/manta/led/slider',
										((parsimonia.morphC*8).round>i).asInteger.asMantaLedColor,
										1,
										128>>i
									);
								};
							},
						);
					});
				},

				3, { // action recorder
					if(v != 0, {
						buttonTimeOuts[i] = false;
						buttonTimers[i] = AppClock.timer(0.12, false, {
							buttonTimeOuts[i] = true;
							buttonStates[i] = 2;
							parsimonia.actionRecorder.record(true);
							manta.sendMsg('/manta/led/button', buttonStates[i].asMantaLedColor, i);
						});
					}, {
						buttonTimers[i].cancel;
						if(buttonTimeOuts[i].not, {
							buttonStates[i] = buttonStates[i].asBoolean.not.asInteger;
							if(parsimonia.actionRecorder.recording, {parsimonia.actionRecorder.record(false)});
							parsimonia.actionRecorder.play(buttonStates[i].asBoolean);
							manta.sendMsg('/manta/led/button', buttonStates[i].asMantaLedColor, i);
						});
					});
				}
			);
		}, '/manta/velocity/button', recvPort: recvPort);

		sliders = OSCFunc({|msg|
			var i = msg[1], v;
			// the if statement filters out the 65635 when a slider is released
			if(msg[2] != 65535, {sliderValues[i] = msg[2].clip(100, 4000)}, {sliderValues[i]});
			v = sliderValues[i];
			switch(i,
				0, {
					switch(buttonStates[0],
						0, {parsimonia.vol_(v.linlin(100, 4000, 0, 1))},
						1, {parsimonia.djFilter_(v.linlin(100, 4000, 0, 1))},
						2, {parsimonia.saturation_(v.linlin(100, 4000, 0, 1))}
					);
				},
				1, {
					switch(buttonStates[2],
						0, {parsimonia.morphA_(v.linlin(100, 4000, 0, 1))},
						1, {parsimonia.morphB_(v.linlin(100, 4000, 0, 1))},
						2, {parsimonia.morphC_(v.linlin(100, 4000, 0, 1))}
					);
				}
			);
		}, '/manta/continuous/slider', recvPort: recvPort);

		// pedal that freezes the currently held pads
		MIDIdef.cc(\pedal, {|val|
			var l = currentLayer;
			pedal = val.asBoolean;
			if(pedal, {
				padStates[l].do{|r, y|
					r.do{|c, x|
						if(padToggles[l][y][x].not, {
							padSostenuto[l][y][x] = c;
							if(padColorInvert[l][y][x])
							{
								c = c.not;
								manta.sendMsg('/manta/led/pad', (c.asInteger+2).asMantaLedColor, (y * 8) + x);
							}
							{manta.sendMsg('/manta/led/pad', (c.asInteger*2).asMantaLedColor, (y * 8) + x)};
						});
					};
				};
			}, {
				padSostenuto[l].do{|r, y|
					r.do{|c, x|
						if(padToggles[l][y][x].not, {
							if(c, {
								parsimonia.playModule(l, y, x, false);
								padSostenuto[l][y][x] = false;
								padStates[l][y][x] = false;
							});
						});
					};
				};
			}
			);
		}, 64);
	}

	updatePads {|layer|
		var l = layer;
		padStates[l].do{|r, x|
			r.do{|c, y|
				if(padSostenuto[l][x][y]) {
					manta.sendMsg('/manta/led/pad', 'red', (x * 8) + y);
				} {
					manta.sendMsg('/manta/led/pad', c.asInteger.asMantaLedColor, (x * 8) + y);
				};
			}
		};
	}

	update {|...args|
		switch(args[1],
			\playModule, {
				var color;
				if((padColorInvert[args[2]][args[3]][args[4]]))
				{color = args[5].not}
				{color = args[5]};
				manta.sendMsg('/manta/led/pad', color.asInteger.asMantaLedColor, (args[3] * 8) + args[4])
			},
			\vol, {
				var v = args[2];
				if(buttonStates[0] == 0, {
					8.do{|i|
						manta.sendMsg(
							'/manta/led/slider',
							((v*8).round>i).asInteger.asMantaLedColor,
							0,
							128>>i
						);
					};
				});
			},
			\djFilter_, {
				var v = args[2];
				if(buttonStates[0] == 1, {
					8.do{|i|
						manta.sendMsg(
							'/manta/led/slider',
							((v*8).round>i).asInteger.asMantaLedColor,
							0,
							128>>i
						);
					};
				});
			},
			\saturation_, {
				var v = args[2];
				if(buttonStates[0] == 2, {
					8.do{|i|
						manta.sendMsg(
							'/manta/led/slider',
							((v*8).round>i).asInteger.asMantaLedColor,
							0,
							128>>i
						);
					};
				});
			},
			\morphA_, {
				var v = args[2];
				if(buttonStates[2] == 0, {
					8.do{|i|
						manta.sendMsg(
							'/manta/led/slider',
							((v*8).round>i).asInteger.asMantaLedColor,
							1,
							128>>i
						);
					};
				});
			},
			\morphB_, {
				var v = args[2];
				if(buttonStates[2] == 1, {
					8.do{|i|
						manta.sendMsg(
							'/manta/led/slider',
							((v*8).round>i).asInteger.asMantaLedColor,
							1,
							128>>i
						);
					};
				});
			},
			\morphC_, {
				var v = args[2];
				if(buttonStates[2] == 2, {
					8.do{|i|
						manta.sendMsg(
							'/manta/led/slider',
							((v*8).round>i).asInteger.asMantaLedColor,
							1,
							128>>i
						);
					};
				});
			},
		);
	}
}