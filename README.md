# Parsimonia - a modular DMI centered around the Snyderphonics Manta controller
_Parsimonia_ is a digital musical instrument (DMI) written in the _SuperCollider_ programming language, centered around the [Snyderphonics Manta](https://snyderphonics.com/manta.htm) controller.
This system was developed as a part of my [PhD thesis](https://ltu.diva-portal.org/smash/record.jsf?dswid=4265&pid=diva2%3A1952531&c=11&searchType=LIST_COMING&language=en&query=&af=%5B%5D&aq=%5B%5B%5D%5D&aq2=%5B%5B%5D%5D&aqe=%5B%5D&noOfRows=50&sortOrder=author_sort_asc&sortOrder2=title_sort_asc&onlyFullText=false&sf=all) entitled *The Act of Patching: Musicking with modular systems*, and thus, there's documentation of both the system, and of use case scnearios to be found in [the book](https://ltu.diva-portal.org/smash/record.jsf?dswid=4265&pid=diva2%3A1952531&c=11&searchType=LIST_COMING&language=en&query=&af=%5B%5D&aq=%5B%5B%5D%5D&aq2=%5B%5B%5D%5D&aqe=%5B%5D&noOfRows=50&sortOrder=author_sort_asc&sortOrder2=title_sort_asc&onlyFullText=false&sf=all), and on the adherent [Research Catalogue Exposition](https://www.researchcatalogue.net/view/3411062/3418320).

As it stands. the system is pretty useless without a _Manta_ controller, but if you're lucky enough to have one, you're welcome to try it out.

## The Instrument
In the _Parsimonia_ system, each hexagonal pad of the _Manta_ can be assigned one or more predefined modules of different types that generate or process sound or data. These module types include synths or samplers, SuperCollider patterns, hardware inputs, effects, MIDI note or MIDI control change, control voltage, and internal parameter control. A module can also be a whole composition, but those are not included in this release.

The surface sensors in the pads always control the most significant module parameter, i.e., amplitude or modulation amount. While the pads functionality remains open in regard to user-assigned modules, the buttons and sliders have fixed functions. The two left-most buttons switch between three different slider modes, indicated by their three color states, off, amber, and red. As such, the sliders and the buttons next to it work in pairs. Depending on the corresponding button's state, the top slider controls functions common for the whole system, i.e., processing applied to the master output. This entails a master volume control, a DJ style combined high and lowpass filter, and saturation. The second slider sets parameters assigned to three different _morph groups_, to which parameters can be assigned with individual ranges.

The top-right button selects between three pages for the 48 pads, again indicated by the three colors, which makes it possible to have up to 144 active pads if necessary. A long press on this button enters a special utility page intended for global functions and possibly presets for setups in a future release.

A long press on the bottom right button activates an _action recorder_ that records the following performance actions on the pads. A short press stops the recording and then toggles between playing back the recorded actions (in a loop) or stopping.

The instrument also includes an optional switch pedal that behaves similarly to a sostenuto pedal on a grand piano (the middle pedal) – i.e., it freezes the currently pressed pads in their current state until the pedal is released. This allows for more complex performances where, for example, several pads with effect modules could be held down with the pedal, freeing up the performer's hands to play on other pads.

![‎parsimoniaMantaLayout](https://github.com/user-attachments/assets/7e727d89-93f3-4163-b6d5-a7120b7c5afd)

## Installation
First, you need to install [SuperCollider](https://supercollider.github.io/) and [SC3-Plugins](https://supercollider.github.io/sc3-plugins/).
In order to use SuperCollider's Quarks package manager, you might also need to install Git. In this case, follow the instructions on the [Git WebPage](https://git-scm.com/downloads).
You can check that the Quarks system works by selecting Quarks from the Language menu in SuperCollider. If the window is populated with packages it should work.
Then, open up a SuperCollider document and run the following line (by placing the cursor on that line and pressing cmd+rtn (Mac) or ctrl+rtn (Win/Linux):
```
Quarks.install("https://github.com/mattiaspetersson/parsimonia")
```
Wait for the installation to finish (watch the post window) and re-compile the class library (Language --> Recompile Class Library).

For the _Manta_ communication I have used the MantaOSC by Spencer Russell and Jeff Snyder. It is running in the background and converts the Manta touches into OSC messages, further handled in SuperCollider.
This has to be built from source, and I have made a fork of [the MantaOSC repository](https://github.com/mattiaspetersson/libmanta/tree/master/MantaOSC) here. Follow the instructions to build MantaOSC for your system, and make sure it is running whenever you want to play your Manta.

## Basic Usage
Start Parsimonia by running
```
p = Parsimonia();
```

The server should boot up, indicated by the display turning green in the lower right corner.
Since Parsimonia is an open, modular system, the next step is to set it up to do stuff. The ```addModule``` method is how you assign modules to pads. This method takes the arguments ```layer, row, col, name, keyAsset, posAsDegree, transpose, localFx, resumePlayback, quant```. The layer, row, and col correspond to the respective _Manta_ pad. Each module also needs a unique name. Some module types need additional parameters to be set which is supplied with the _keyAsset_ argument. This includes, for example, sample player modules which need file paths for samples (as seen in the example below). If the ```posAsDegree``` argument is set to ```true```, the position of the pad on the _Manta_ will affect the module's pitch according to the currently selected scale. The module can also be transposed, as shown in the beginning rows of the example, where each pad on the first row of layer one (layers, rows, and columns are zero-indexed) is assigned a triad of sine wave oscillators each. Modules can also act as local effects, they can resume their playback from where they left of (in case of samples and patterns), and they can also be quantized to beats (or fractions of beats) according to the current tempo.

Unless the ```localFx``` argument is ```true```, effect processing modules appear as global, meaning that they are placed in a separate group after the sound generators. In the example, ight different such global effects are assigned to the top row of pads. Pressing these pads will thus apply the effect to all sound generators (unless the effect send is explicitly turned of in a generator module).

The example also shows how parameters can be assigned to morph groups with the ```assignToMorphGroup``` method. Here, the frequency parameter of the module named sine1 is assigned to morph group B with values ranging from 16.5 to 219 Hz, following an exponential curve. Further down in the code morph A is assigned to control the speed of a beat repeat effect.
```
(
p.tempo_(126);
8.do{|i|
    p.addModule(1, 0, i, \sine1, \Sine, nil, true, -12);
    p.addModule(1, 0, i, \sine2, \Sine, nil, true, 3);
    p.addModule(1, 0, i, \sine3, \Sine, nil, true, 5);
    p.addModule(1, 0, i, \fbSet, \Set, [\fb, [0, 0.9, -5].asSpec]);
    p.modules[1][0][i][\sine1].assignToMorphGroup(
        \morphB, \freq, [16.5, 219, \exp].asSpec
    );
};

// samples
p.addModule(0, 1, 0, \triLo,
    \SamplePlayerCF,
    "/Users/mattpete/Dropbox/work/ljudarkivet/empTriangleRawLo.wav",
    resumePlayback: true
).setModulePar(0, 1, 0, \amp, 0.3);

p.addModule(0, 1, 1, \triHi,
    \SamplePlayerCF,
    "/Users/mattpete/Dropbox/work/ljudarkivet/empTriangleRawHi.wav",
    resumePlayback: true
).setModulePar(0, 1, 1, \amp, 0.1);

// fx
p.addModule(0, 5, 0, \reverb, \Reverb);
p.addModule(0, 5, 2, \shuffler, \Shuffler);
p.addModule(0, 5, 7, \repeater, \Repeater);
p.modules[0][5][7][\repeater].assignToMorphGroup(
    \morphA, \repeats, [1, 32, \exp].asSpec
);
)
```

## Extending the system
The system can be easily extended with new modules. They need to inherit from the ```ParsimoniaModule``` class, and should follow this structure:
```
// a synth module
ParsimoniaModule_Sine : ParsimoniaModule {
   initModule {
      type = \synth;
         name = \sine;
      }

   *build {
      ParsimoniaModule.synthFactory(\sine, {|buf, freq, amp, gate, loop,
         fb|
            SinOscFB.ar(freq!2, fb, 0.1);
      });
   }
}

// an effect module
ParsimoniaModule_Saturation : ParsimoniaModule {
   initModule {
      type = \fx;
         name = \saturation;
      }

   *build {
      ParsimoniaModule.fxFactory(\saturation, {|input, freq, gate,
         saturation = 9|
         var sig, satVal;
         satVal = saturation.lag(0.1).clip(1.0, 99.0);
         sig = (input * satVal).tanh / (satVal ** 0.6); // formula by James Harkins (satVal can't be 0!)
      });
   }
}
```

## Disclaimer
Parsimonia has been used on Mac and Linux systems, intended for quite specific, artistic, use cases. As such, it *might* be useful for other people, but I cannot guarantee that it will be.
