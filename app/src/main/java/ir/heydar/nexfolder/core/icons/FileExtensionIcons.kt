package ir.heydar.nexfolder.core.icons

/**
 * Maps file extensions to their [FileIconKind].
 *
 * The table covers the usual extension dump you get from a file server (hundreds of
 * spellings, typos and version suffixes) and degrades gracefully: anything unknown is
 * reported as `null` so the caller can fall back to the generic type icon or a
 * generated extension badge.
 */
object FileExtensionIcons {

    private val byExtension: Map<String, FileIconKind> = buildMap {
        // === Documents ===
        assign(
            FileIconKind.PDF,
            "pdf", "pdfx"
        )
        assign(
            FileIconKind.WORD,
            "doc", "docx", "docm", "dot", "dotx", "dotm", "odt", "ott", "rtf", "rtfd",
            "wpd", "wps", "wri", "pages", "tex", "latex", "ltx", "abw", "hwp", "hwpx",
            "sxw", "stw", "uot", "xps", "oxps", "ps", "mw", "wpt", "sdw", "vor", "nfo",
            "pub"
        )
        assign(
            FileIconKind.SPREADSHEET,
            "xls", "xlsx", "xlsm", "xlsb", "xslx", "xlt", "xltx", "xltm", "ods", "ots",
            "csv", "tsv", "numbers", "gnumeric", "sxc", "stc", "uos", "dif", "slk",
            "xslx"
        )
        assign(
            FileIconKind.PRESENTATION,
            "ppt", "pptx", "pptm", "pps", "ppsx", "ppsm", "pot", "potx", "potm", "odp",
            "otp", "sxi", "sti", "uop", "show", "sdd", "dps"
        )
        assign(
            FileIconKind.MARKDOWN,
            "md", "markdown", "mdown", "mkd", "mkdn", "mdwn", "rst", "rdoc"
        )
        assign(
            FileIconKind.TEXT,
            "txt", "text", "log", "log1", "log2", "readme", "read", "me", "diz", "lst",
            "ans", "list", "sample", "doc.txt", "err", "dic"
        )
        assign(
            FileIconKind.EBOOK,
            "epub", "mobi", "azw", "azw3", "azw4", "kf8", "fb2", "lit", "djvu", "djv",
            "cbz", "cbr", "cb7", "cbt", "ibooks", "opf", "prc", "tcr", "nc",
            "chm", "hlp", "daisy"
        )

        assign(
            FileIconKind.FONT,
            "ttf", "otf", "ttc", "tte", "otc", "woff", "woff2", "eot", "fon", "pfb",
            "pfm", "afm", "bdf", "pcf", "dfont", "sfd"
        )

        // === Code & data ===
        assign(
            FileIconKind.CODE,
            "js", "mjs", "cjs", "js2", "jsx", "ts", "tsx", "kt", "kts", "java", "class",
            "cs", "csproj", "cs2", "sln", "vb", "vbproj", "fs", "fsx", "c", "h", "cc",
            "cpp", "cxx", "hpp", "hxx", "hh", "inl", "go", "rs", "rb", "rhtml", "rjs",
            "py", "pyc", "pyo", "pyd", "pyw", "pl", "pm", "plx", "php", "php1", "php2",
            "php3", "php4", "php5", "php6", "php7", "php8", "phps", "phpt", "phpx",
            "phpl", "inc", "lua", "tcl", "swift", "m", "mm", "scala", "sc", "dart",
            "groovy", "gradle", "mq4", "mq5", "sol", "pas", "pp", "elm", "ex", "exs",
            "erl", "hrl", "hs", "lhs", "clj", "cljs", "cljc", "coffee", "asm", "s",
            "nasm", "r", "jl", "nim", "zig", "v", "sv", "svh", "vhdl", "vy", "f", "f90",
            "f95", "for", "ada", "adb", "ads", "cob", "cbl", "prg", "bas", "au3",
            "xml", "xsl", "xslt", "xsd", "wsdl", "dtd", "rss", "atom", "opml", "xqy",
            "xsp", "sitemap", "resx", "resw", "rc", "as", "diff", "patch", "po", "jad",
            "jnlp", "rdf", "cdf", "scc", "vspscc", "vssscc", "suo"
        )
        assign(
            FileIconKind.SCRIPT,
            "sh", "bash", "zsh", "fish", "ksh", "csh", "tcsh", "bat", "cmd", "ps1",
            "psm1", "vbs", "vbe", "wsf", "wsh", "ahk", "command", "run", "action",
            "action2", "cgi", "fcgi", "acgi", "job", "ws", "wsc"
        )
        assign(
            FileIconKind.WEB,
            "html", "htm", "xhtml", "xhtml5", "xhtm", "xht", "shtml", "shtm", "stm",
            "stml", "dhtml", "htc", "hta", "htx", "htlm", "hmtl", "html1", "html4",
            "html5", "bhtml", "bml", "mhtml", "mht", "webarchive", "asp", "aspx",
            "asax", "ascx", "ashx", "asmx", "axd", "asd", "aspg", "aspl", "aspp",
            "asps", "aspxx", "asxp", "cfm", "cfml", "cfc", "cfx", "jsp", "jspx", "jspf",
            "jsf", "jhtml", "jhtm", "do", "phtml", "phtm", "ssi", "wml", "hdml", "css",
            "scss", "sass", "less", "styl", "twig", "mustache", "hbs", "ejs", "pug",
            "njk", "liquid", "asa", "asp1", "asp2", "apsx", "master", "dwt", "lbi",
            "ctp", "ftl", "vtl", "gsp", "jspa", "seam", "lasso", "lassoapp", "woa",
            "svc", "xsql", "thtml", "ihtml", "ghtml", "ehtml", "alhtm", "bfhtm",
            "prhtm", "wihtm", "itml", "hml", "htn", "hts", "htmls", "zhtml", "ztml"
        )
        assign(
            FileIconKind.CONFIG,
            "ini", "cfg", "conf", "config", "properties", "env", "toml", "yaml", "yml",
            "htaccess", "htpasswd", "p3p", "pac", "reg", "inf", "plist", "dist", "settings",
            "theme", "skin", "tpl", "template", "xconf", "service", "cnf", "editorconfig",
            "gitignore", "gitattributes", "gitmodules", "gitkeep", "npmrc", "babelrc",
            "eslintrc", "dockerignore", "lock", "desktop", "key", "identity",
            "pem", "crt", "cer", "csr", "pfx", "p12", "p7b", "pgp", "gpg", "asc", "pvk",
            "sln.config", "lic", "lck", "tmpl"
        )
        assign(
            FileIconKind.JSON,
            "json", "json5", "jsonc", "jsonl", "ndjson", "geojson", "har", "webmanifest",
            "ipynb", "avsc", "map"
        )
        assign(
            FileIconKind.DATABASE,
            "sql", "db", "db3", "sqlite", "sqlite3", "s3db", "mdb", "accdb", "accde",
            "accdr", "dbf", "dbm", "dbml", "fp7", "fp5", "mdf", "ndf", "ldf", "sdf",
            "dsn", "myd", "myi", "frm", "ibd", "dmp", "psql", "pgsql", "pdb",
            "sqldump", "mydump", "nsf", "btr"
        )

        // === Communication & schedules ===
        assign(
            FileIconKind.EMAIL,
            "eml", "emlx", "msg", "mbox", "mbx", "oft", "pst", "ost", "mail", "email",
            "nded-pga-emial", "dbx"
        )
        assign(FileIconKind.CALENDAR, "ics", "ifb", "ical", "vcs", "calendar")
        assign(FileIconKind.CONTACT, "vcf", "vcard", "ldif", "abbu", "contact")
        assign(FileIconKind.TORRENT, "torrent")
        assign(FileIconKind.MAP, "kml", "kmz", "gpx", "geo", "loc", "fit", "osm")

        // === Design & 3D ===
        assign(
            FileIconKind.DESIGN,
            "psd", "psb", "ai", "eps", "ep", "indd", "idml", "indt", "cdr", "cdrx", "cmx",
            "xcf", "sketch", "fig", "afdesign", "afphoto", "afpub", "kra", "ora", "clip",
            "sai", "sai2", "pspimage", "pxz", "mdp", "fla", "xfl", "psp", "qxd", "jbf"
        )
        assign(
            FileIconKind.MODEL_3D,
            "obj", "fbx", "stl", "blend", "blend1", "3ds", "max", "dae", "ply", "gltf",
            "glb", "skp", "lwo", "lws", "ma", "mb", "c4d", "3dm", "step", "stp", "iges",
            "igs", "f3d", "3mf", "x3d", "x3dv", "wrl", "vrml", "usdz", "usd", "usda",
            "usdc", "abc", "scn"
        )
        assign(
            FileIconKind.CAD,
            "dwg", "dxf", "dwf", "dwfx", "dgn", "rvt", "rfa", "rft", "sldprt", "sldasm",
            "slddrw", "ipt", "iam", "catpart", "catproduct", "prt", "drw", "sch", "brd"
        )

        // === System & binaries ===
        assign(
            FileIconKind.DISK,
            "iso", "img", "dmg", "vmdk", "vhd", "vhdx", "vdi", "hdd", "qcow", "qcow2",
            "ova", "ovf", "wim", "swm", "esd", "nrg", "cue", "toast", "dsk", "adf", "d64",
            "cso", "sparsecimage", "e01", "dd", "ccd", "sub", "vmx", "vmsd", "vmsn",
            "sparseimage", "dmgpart"
        )
        assign(
            FileIconKind.EXECUTABLE,
            "exe", "msi", "msix", "msixbundle", "appx", "appxbundle", "appinstaller",
            "com", "scr", "cpl", "elf", "appimage", "exe2", "dllx", "application",
            "msp", "mst"
        )
        assign(
            FileIconKind.LIBRARY,
            "dll", "so", "dylib", "a", "lib", "o", "ocx", "ko", "framework", "bundle",
            "exp", "nupkg", "vsix"
        )
        assign(
            FileIconKind.BINARY,
            "dat", "data", "bin", "dump", "hex", "md5", "sha1", "sha256", "sha512",
            "crc", "sfv", "dbg", "sym", "sys", "drv", "efi", "rom", "sav", "save",
            "state", "blob", "pak", "pk3", "zdat", "sdb", "ser", "out", "so.bin", "core"
        )
        assign(
            FileIconKind.PACKAGE,
            "deb", "rpm", "pkg", "mpkg", "snap", "flatpak", "flatpakref", "ebuild",
            "xbps", "xar", "abuild", "sis", "sisx", "apkm"
        )
        assign(
            FileIconKind.LINK,
            "lnk", "url", "webloc", "alias", "internet", "website", "bookmark",
            "link"
        )
        assign(
            FileIconKind.BACKUP,
            "bak", "bak2", "bck", "bk", "bkf", "bkp", "bu", "backup", "old", "old1",
            "old2", "old3", "older", "orig", "ori", "original", "new", "tmp", "temp",
            "tem", "swp", "swo", "part", "partial", "crdownload", "sbk"
        )
        assign(FileIconKind.APK, "apk", "apks", "xapk", "xap", "ipa", "aab")
        assign(FileIconKind.PLAYLIST, "m3u", "m3u8", "pls", "xspf", "wpl", "asx", "wvx", "mpcpl", "apl")

        // === Audio ===
        assign(
            FileIconKind.AUDIO_MP3,
            "mp3", "mp2", "mpa", "m4b", "m4p", "m4r", "aax", "aa", "w64"
        )
        assign(
            FileIconKind.AUDIO_WAV,
            "wav", "wave", "aiff", "aif", "aifc", "au", "snd", "ac3", "eac3", "dts",
            "cda", "gsm", "vox", "mmf", "mld"
        )
        assign(FileIconKind.AUDIO_FLAC, "flac", "ape", "wv", "tak", "alac", "mka")
        assign(FileIconKind.AUDIO_AAC, "aac", "adts", "3ga")
        assign(FileIconKind.AUDIO_OGG, "ogg", "oga", "spx", "opus")
        assign(FileIconKind.AUDIO_M4A, "m4a")
        assign(FileIconKind.AUDIO_WMA, "wma", "wax")
        assign(FileIconKind.AUDIO_MID, "mid", "midi", "kar", "rmi", "xmf")
        assign(
            FileIconKind.AUDIO,
            "ra", "ram", "awb", "amr", "sndh", "dss", "caf", "asf.audio", "xm", "mod",
            "s3m"
        )

        // === Archives ===
        assign(
            FileIconKind.ARCHIVE_ZIP,
            "zip", "zipx", "jar", "war", "ear", "xpi", "crx", "safariextz", "cbz.raw",
            "org.zip", "tar.zip"
        )
        assign(FileIconKind.ARCHIVE_RAR, "rar", "r00", "r01", "rev")
        assign(FileIconKind.ARCHIVE_7Z, "7z", "cb7.raw")
        assign(FileIconKind.ARCHIVE_TAR, "tar", "cpio")
        assign(FileIconKind.ARCHIVE_GZ, "gz", "gzip", "tgz", "tbz", "tbz2", "bz2", "xz", "txz", "bz")
        assign(
            FileIconKind.ARCHIVE,
            "lz", "lzma", "lzo", "zst", "z", "cab", "arj", "lha", "lzh", "ace", "sit",
            "sitx", "sea", "hqx", "cpt", "zoo", "zpaq", "zst.raw", "rpm.zst", "arc",
            "dms"
        )

        // === Media fallbacks ===
        assign(
            FileIconKind.VIDEO,
            "wmv", "ogv", "ogm", "rm", "rmvb", "asf", "divx", "vob", "f4v", "m2ts",
            "mts", "mpe", "m1v", "m2v", "3g2", "dv", "mxf", "qt", "wtv", "nsr", "roq",
            "mpv", "yuv", "avchd", "swf", "smi", "viv", "ivf", "dcr", "dir", "dxr",
            "smil"
        )
        assign(
            FileIconKind.IMAGE,
            "raw", "cr2", "cr3", "nef", "arw", "dng", "orf", "raf", "rw2", "pef", "srw",
            "x3f", "jp2", "j2k", "jxl", "pcx", "tga", "dds", "exr", "hdr", "ppm", "pgm",
            "pbm", "pnm", "xbm", "xpm", "wmf", "emf", "icns", "jpe", "jfif", "jfi",
            "jif", "ani", "cur", "svgz", "psdt", "gifv", "wbmp", "art", "jpf", "jpx",
            "jps"
        )
    }

    /** Resolves the icon family for [extension] (with or without a leading dot). */
    fun resolve(extension: String): FileIconKind? {
        val key = normalize(extension) ?: return null
        return byExtension[key]
            ?: byExtension[key.substringAfterLast('.')]
            ?: byExtension[key.substringBeforeLast('.')]
    }

    fun isKnown(extension: String): Boolean = resolve(extension) != null

    /** Lower-cases, strips stray punctuation and rejects obvious scan noise. */
    private fun normalize(raw: String): String? {
        val cleaned = raw.lowercase().trim { !it.isLetterOrDigit() }
        if (cleaned.isEmpty() || cleaned.length > 12) return null
        return cleaned
    }

    private fun MutableMap<String, FileIconKind>.assign(
        kind: FileIconKind,
        vararg extensions: String
    ) {
        extensions.forEach { extension -> put(extension, kind) }
    }
}
