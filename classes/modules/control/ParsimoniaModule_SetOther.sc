ParsimoniaModule_SetOther : ParsimoniaModule {
	// the key asset should be an array of 2 arrays: [[layer, col, row, name], [par, spec]]
	initModule {
		type = \set;
		name = \setOther;
		if(keyAsset.notNil, {
			if(keyAsset.isArray, {
				keyAsset[1].pairsDo{|par, spec|
					if(spec.isKindOf(ControlSpec), {
						this.addControlSpec(par, spec);
					});
				};
			});
		});
	}

	setModulePar {|par, val|
		assets[\controlSpecs].keysValuesDo{|key, spec|
			parsimonia.modules[keyAsset[0][0] ? layer][keyAsset[0][1] ? row][keyAsset[0][2] ? col][keyAsset[0][3]].do{|module|
				if(module.type != \set, {
					module.setModulePar(key, spec.map(val));
				});
			};
		};
	}
}