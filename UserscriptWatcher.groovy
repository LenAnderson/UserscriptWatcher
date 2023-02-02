import java.io.*
import java.net.*
import java.nio.file.*
import com.sun.nio.file.*

class UserscriptWatcher {
	static main (args) {
		List roots = []
		Root root
		args.each{arg->
			if (arg[0..1] == '--' && root) {
				String argName = arg[2..-1].split('=', 2)[0]
				switch (argName) {
					case 'name':
						root.name = arg.split('=', 2)[1]
						break
				}
			} else {
				root = new Root(arg)
				roots += root
			}
		}
		def uw = new UserscriptWatcher(roots)
	}

	static class Root {
		String path
		String name

		Root(String path) {
			this.path = path
		}

		String getName() {
			name ?: new File(path).name
		}
	}
	
	
	
	
	Map includes = [:]
	Map imports = [:]
	Map compiling = [:]
	Map again = [:]
	List<Root> roots = []
	
	UserscriptWatcher(roots) {
		this.roots = roots
		
		initWatcher()
	}
	
	def initWatcher() {
		println ""
		this.roots.each { root ->
			println "$root.path  -->  $root.name"
			Path path = Paths.get("${root.path}/src")
			WatchService ws = FileSystems.default.newWatchService()
			
			path.register(
				ws,
				[
					StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_CREATE
				] as WatchEvent.Kind<?>[],
				ExtendedWatchEventModifier.FILE_TREE
			)

			println "\nREADY\n"
			
			while (true) {
				WatchKey key = ws.take()
				
				key.pollEvents().each{ event ->
					def kind = event.kind()
					println "$kind ${event.context()}"
					compile(root)
				}
				
				def valid = key.reset()
				if (!valid)
					break
			}
		}
	}
	
	
	def compile(root) {
		if (compiling[root]) {
			again[root] = true
			return
		}
		compiling[root] = true
		
		def base = new File(new File("${root.path}/src/script.js").canonicalPath)
		def compiled = new File(new File("${root.path}/${root.name}.user.js").canonicalPath)
		def compiledDev = new File(new File("${root.path}/${root.name}-DEV.user.js").canonicalPath)
		
		includes[base] = []
		def compiledText = getCompiled(base, base).replaceAll(~/(?m)^(\s*)\/\/\s*\$\{imports\}$/, { str, match ->
			def text = ""
			text += "${match[1]}// ---------------- IMPORTS  ----------------\n"
			imports.each{ path, content ->
				text += "\n\n${match[1]}// ${Paths.get(new File(root.path).canonicalPath).relativize(Paths.get(path))}\n"
				text += content
			}
			text += "${match[1]}// ---------------- /IMPORTS ----------------\n"
			return text
		})
		compiled.setText(compiledText, 'UTF-8')
		compiledDev.setText(compiledText.replaceAll(~/(?m)^(\/\/\s*@version\s+).+$/, "\$1${UUID.randomUUID()}"), 'UTF-8')
		
		compiling[root] = false
		if (again[root]) {
			again[root] = false
			compile(root)
		}
	}
	
	def getCompiled(base, root) {
		println "getCompiled: $base"
		base.getText('UTF-8')
			.replaceAll(~/(?m)^export /, '')
			.replaceAll(~/(?m)^import .+? from "([^"]+?)";$/, { str, match ->
				File inc = new File(new File("${base.parent}/${match}").canonicalPath)
				String replace = "// !!! CANNOT FIND: ${inc.canonicalPath}"
				if (inc.exists()) {
					if (!includes[root].contains(inc.canonicalPath)) {
						includes[root] << inc.canonicalPath
						imports[inc.canonicalPath] = getCompiled(inc, root)
						replace = ""
					} else {
						replace = ""
					}
				}
				replace
			})
			.replaceAll(~/(?:\s*\/\/\s*)?\$\{include: ([^{}]+)\}/, { str, match ->
				File inc = new File(new File("${base.parent}/${match}").canonicalPath)
				String replace = "// !!! CANNOT FIND: ${inc.canonicalPath}"
				if (inc.exists()) {
					includes[root] << inc.canonicalPath
					replace = getCompiled(inc, root)
				}
				replace
			})
			.replaceAll(~/(?:\s*\/\/\s*)?\$\{include-([a-z0-9\-]+): ([^{}]+)\}/, { str, opts, match ->
				def options = opts.split('-')
				File inc = new File(new File("${base.parent}/${match}").canonicalPath)
				String replace = "// !!! CANNOT FIND: ${inc.canonicalPath}"
				if (inc.exists()) {
					if (!options.find{it=="once"} || !includes[root].contains(inc.canonicalPath)) {
						includes[root] << inc.canonicalPath
						replace = getCompiled(inc, root)
						if (options.find{it=="min"}) replace = replace.replaceAll(~/[\r\n\t]/, '')
						if (options.find{it=="esc"}) replace = replace.replaceAll(~/([''""])/, '\\\\$1')
						if (options.find{it=="b64"}) replace = "data:${Files.probeContentType(inc.toPath())};base64,${inc.bytes.encodeBase64().toString()}"
					} else {
						replace = ""
					}
				}
				replace
			})
	}
}
