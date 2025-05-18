ParsimoniaModule_Set : ParsimoniaModule {
	initModule {
		type = \set;
		name = \set;
		if(keyAsset.notNil, {
			if(keyAsset.isArray, {
				keyAsset.pairsDo{|par, spec|
					if(spec.isKindOf(ControlSpec), {
						this.addControlSpec(par, spec);
					});
				};
			});
		});
	}

	setModulePar {|par, val|
		assets[\controlSpecs].keysValuesDo{|key, spec|
			parsimonia.modules[layer][row][col].keysValuesDo{|name, module|
				if(module.type != \set, {
					module.setModulePar(key, spec.map(val));
				});
			};
		};
	}
}