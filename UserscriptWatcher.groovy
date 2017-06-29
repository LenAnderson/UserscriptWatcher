import java.io.*
import java.net.*
import java.nio.file.*
import com.sun.nio.file.*

class UserscriptWatcher {
	static main (args) {
		def uw = new UserscriptWatcher(args)
	}
	
	
	
	
	def includes = [:]
	def compiling = [:]
	def again = [:]
	def roots = []
	
	UserscriptWatcher(roots) {
		this.roots = roots
		
		initWatcher()
	}
	
	def initWatcher() {
		this.roots.each { root ->
			Path path = Paths.get("${root}/src")
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
			
			while (true) {
				WatchKey key = ws.take()
				
				key.pollEvents().each{ event ->
					def kind = event.kind()
					println "$kind ${event.context}"
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
		
		def base = new File("$root/src/script.js")
		def compiled = new File("$root/${new File(root).name}.user.js")
		
		includes[base] = []
		compiled.setText(getCompiled(base, base), 'UTF-8')
		
		compiling[root] = false
		if (again[root]) {
			again[root] = false
			compile(root)
		}
	}
	
	def getCompiled(base, root) {
		base.text.replaceAll(~/\$\{include: ([^{}]+)\}/, { str, match ->
				File inc = new File("${base.parent}/${match}")
				String replace = "// !!! CANNOT FIND: ${inc.absolutePath}"
				if (inc.exists()) {
					includes[root] << inc.absolutePath
					replace = getCompiled(inc, root)
				}
				replace
			})
			.replaceAll(~/\$\{include-([a-z0-9\-]+): ([^{}]+)\}/, { str, opts, match ->
				def options = opts.split('-')
				File inc = new File("${base.parent}/${match}")
				String replace = "// !!! CANNOT FIND: ${inc.absolutePath}"
				if (inc.exists()) {
					if (options.find{it=="once"} && !includes[root].contains(inc.absolutePath)) {
						includes[root] << inc.absolutePath
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
