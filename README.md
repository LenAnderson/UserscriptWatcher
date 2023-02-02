# UserscriptWatcher
Groovy script (Groovy 3) to autmatically compile a collection of files into a userscript.


## How to use?
1. Create a directory for your userscript. The directory's name will be used to name the compiled file.
2. In that directory add a subdirectory named `src`. This is where you put all the files and folders that will be combined to make the userscript.
3. In the `src` directory add a file named `script.js` The UserscriptWatcher will start with this file.
4. Run UserscriptWatcher.groovy with the path to your userscript's directory as the argument.

### Running UserscriptWatcher.groovy
```bash
UserscriptWatcher.groovy "path/to/My-Script"
```
```bash
groovy UserscriptWatcher.groovy "path/to/My-Script" "path/to/My-Other-Script"
```
Specifying the name of the compiled userscript file.
```bash
# will compile to My-Script.user.js
UserscriptWatcher.groovy "path/to/My-Script"

# will compile to my_script.user.js
UserscriptWatcher.groovy "path/to/My-Script" --name=my_script

# will compile to my_script.user.js and My-Other-Script.user.js
UserscriptWatcher.groovy "path/to/My-Script" --name=my_script "path/to/My-Other-Script"
```

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
- `${include-min}` will put the contents of that file and remove all linebreaks and tabstops (`\r`, `\n`, `\t`)
- `${include-esc}` will put the contents of that file and escape single and double quotes (`'`, `"`)
- `${include-b64}` will put the contents of that file as a base64 data string (`data:contentType;base64,...`)
- ES6 module imports are supported as well and will replace `${imports}` in the source file.
  - `import { SomeThing } from './file.js'`

The modifiers (`-once`, `-min`, `-esc`, `-b64`) can be combined, e.g. `${include-once-min: file.js}`.

##### Example

`script.js`:
```javascript
// ==UserScript==
// @name         My-Script
// @namespace    https://github.com/LenAnderson/
// @downloadURL  https://github.com/LenAnderson/My-Script/raw/master/My-Script.user.js
// @version      1.0
// @author       LenAnderson
// @match        http://www.example.com/
// @grant        none
// ==/UserScript==

import { SomeOtherClass } from './someFolder/SomeOtherClass.js';

(function() {
	${include-once: PartOne.js}
	// you can put the include statements in comments to avoid problems with linting / intellisense
	// ${include-once: PartTwo.js}
	
	// ${imports}
	
	let style = document.createElement('style');
	style.innerHTML = '${include-min-esc: css/styles.css}';
	document.body.appendChild(style);
	
	let sc = new SomeClass(); // SomeClass was included in PartOne.js
})();
```

`css/styles.css`:
```css
body {
    background-image: url("${include-b64: ../img/background.png}");
}
```

## Live updates as the code changes
To immediately have access to the recompiled userscript during development I usually use the following setup. This way I only need to refresh the page to use the updates userscript.

Have a page on a local webserver (e.g. XAMPP) that takes the path to a script as a GET parameter and then simply returns that files contents.
```
<?php
echo file_get_contents($_GET['script']);
```

Add a userscript with similar headers that loads the compiled script with `GM_xmlhttpRequest`.
```
// ==UserScript==
// @name         My Script [DEV]
// @match        https://www.example.com/  <--- must be the same @match as in your userscript
// @grant        GM_xmlhttpRequest         <--- additionally you must add all the @grant from your userscript
// ==/UserScript==

(function() {
    GM_xmlhttpRequest({url: 'http://localhost/userscripts.php?script=C:/Path/to/My-Script/My-Script.user.js', onload: (data) => {
        eval(data.responseText);
    }});
})();
