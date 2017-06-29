# UserscriptWatcher
groovy script to autmatically compile a collection of files into a userscript


## How to use?
1. Create a directory for your userscript. The directory's name will be used to name the compiled file.
2. In that directory add a subdirectory named `src`. This is where you put all the files and folders that will be combined to make the userscript.
3. In the `src` directory add a file named `script.js` The UserscriptWatcher will start with this file.
4. Run UserscriptWatcher.groovy with the path to your userscript's directory as the argument.

### Folder Structure
```
.../Documents/Userscripts/
    ├ My-Script/
    │   ├ src/
    │   │   ├ css/
    │   │   │   ├ styles.css
    │   │   │   └ additionalStyles.css
    │   │   ├ img/
    │   │   │   └ background.png
    │   │   ├ partOne.js
    │   │   ├ partTwo.js
    │   │   ├ SomeClass.js
    │   │   ├ AnotherClass.js
    │   │   └ script.js
    │   └ My-Script.user.js   <-- This file will be created and updated by UserscriptWatcher
    └ My-Other-Script/
        ├ src/
        │   └ ...
        └ My-Other-Script.user.js
```

### Including Files
To include another file write `${include: path/to/file.extension}` somewhere in your code and UserscriptWatcher will put the contents of the file in that place. The path is relative to the file you are currently working on.

- `${include: file}` will put the contents of that file
- `${include-once}` will put the contents of that file if it has not been included before
- `${include-min}` will put the contents of that file and remove all linebreaks and tabstops (\r, \n, \t)
- `${include-esc}` will put the contents of that file and escape single and double quotes (', ")
- `${include-b64}` will put the contents of that file as a base64 data string (data:contentType;base64,...)

The modifiers (-once, -min, -esc, -b64) can be combined.

##### Example

`script.js`:
```
// ==UserScript==
// @name         My-Script
// @namespace    https://github.com/LenAnderson/
// @downloadURL  https://github.com/LenAnderson/My-Script/raw/master/My-Script.user.js
// @version      1.0
// @author       LenAnderson
// @match        http://www.example.com/
// @grant        none
// ==/UserScript==

(function() {
	${include-once: PartOne.js}
	${include-once: PartTwo.js}
	
  let style = document.createElement('style');
  style.innerHTML = '${include-min-esc: css/styles.css}';
  document.body.appendChild(style);
  
  let sc = new SomeClass(); // SomeClass was included in PartOne.js
})();
```

`css/styles.css`:
```
body {
    background-image: url("{include-b64: ../img/background.png}");
}
```
