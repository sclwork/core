package com.scliang.core.base;

import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/8.
 */
public class ContentType {
    private ContentType() {}

    /**
     * 根据后缀名(如：apk)获得对应的ContentType
     */
    public static String getContentType(String suffix) {
        return sContentType.get(suffix);
    }

    /**
     * 根据后缀名(如：pdf)判断是否为TextType
     * @param uri
     * @return
     */
    public static boolean isTextMimeType(Uri uri) {
        if (uri == null) {
            return true;
        }

        String extension = "";
        String str = uri.toString();
        String query = uri.getQuery();
        int index = str.indexOf('?');
        if (index >= 0) {
            str = str.substring(0, index);
        }
        int last = str.lastIndexOf('.');
        if (last >= 0) {
            extension = str.substring(last + 1);
        }

        return isTextMimeType(extension);
    }

    /**
     * 根据后缀名(如：pdf)判断是否为TextType
     * @param extension
     * @return
     */
    public static boolean isTextMimeType(String extension) {
        if (TextUtils.isEmpty(extension)) {
            return true;
        }

        MimeTypeMap map = sMimeTypeMap.get(extension);
        if (map == null) {
            return true;
        }

        return map.isTextType;
    }

    private static Map<String, String> sContentType = new HashMap<>();
    static {
        sContentType.put("apk", "application/vnd.android.package-archive");
        sContentType.put("mp3", "audio/mp3");
        sContentType.put("mp4", "video/mpeg4");
        sContentType.put("jpg", "image/jpeg");
        sContentType.put("jpeg", "image/jpeg");
        sContentType.put("png", "image/png");
        sContentType.put("gif", "image/gif");
    }

    private static Map<String, MimeTypeMap> sMimeTypeMap = new HashMap<>();
    static {
        loadEntry("application/andrew-inset", "ez", false);
        loadEntry("application/dsptype", "tsp", false);
        loadEntry("application/futuresplash", "spl", false);
        loadEntry("application/hta", "hta", false);
        loadEntry("application/mac-binhex40", "hqx", false);
        loadEntry("application/mac-compactpro", "cpt", false);
        loadEntry("application/mathematica", "nb", false);
        loadEntry("application/msaccess", "mdb", false);
        loadEntry("application/oda", "oda", false);
        loadEntry("application/ogg", "ogg", false);
        loadEntry("application/pdf", "pdf", false);
        loadEntry("application/pgp-keys", "key", false);
        loadEntry("application/pgp-signature", "pgp", false);
        loadEntry("application/pics-rules", "prf", false);
        loadEntry("application/rar", "rar", false);
        loadEntry("application/rdf+xml", "rdf", false);
        loadEntry("application/rss+xml", "rss", false);
        loadEntry("application/zip", "zip", false);
        loadEntry("application/vnd.android.package-archive", "apk", false);
        loadEntry("application/vnd.cinderella", "cdy", false);
        loadEntry("application/vnd.ms-pki.stl", "stl", false);
        loadEntry("application/vnd.oasis.opendocument.database", "odb", false);
        loadEntry("application/vnd.oasis.opendocument.formula", "odf", false);
        loadEntry("application/vnd.oasis.opendocument.graphics", "odg", false);
        loadEntry("application/vnd.oasis.opendocument.graphics-template", "otg", false);
        loadEntry("application/vnd.oasis.opendocument.image", "odi", false);
        loadEntry("application/vnd.oasis.opendocument.spreadsheet", "ods", false);
        loadEntry("application/vnd.oasis.opendocument.spreadsheet-template", "ots", false);
        loadEntry("application/vnd.oasis.opendocument.text", "odt", false);
        loadEntry("application/vnd.oasis.opendocument.text-master", "odm", false);
        loadEntry("application/vnd.oasis.opendocument.text-template", "ott", false);
        loadEntry("application/vnd.oasis.opendocument.text-web", "oth", false);
        loadEntry("application/vnd.rim.cod", "cod", false);
        loadEntry("application/vnd.smaf", "mmf", false);
        loadEntry("application/vnd.stardivision.calc", "sdc", false);
        loadEntry("application/vnd.stardivision.draw", "sda", false);
        loadEntry("application/vnd.stardivision.impress", "sdd", false);
        loadEntry("application/vnd.stardivision.impress", "sdp", false);
        loadEntry("application/vnd.stardivision.math", "smf", false);
        loadEntry("application/vnd.stardivision.writer", "sdw", false);
        loadEntry("application/vnd.stardivision.writer", "vor", false);
        loadEntry("application/vnd.stardivision.writer-global", "sgl", false);
        loadEntry("application/vnd.sun.xml.calc", "sxc", false);
        loadEntry("application/vnd.sun.xml.calc.template", "stc", false);
        loadEntry("application/vnd.sun.xml.draw", "sxd", false);
        loadEntry("application/vnd.sun.xml.draw.template", "std", false);
        loadEntry("application/vnd.sun.xml.impress", "sxi", false);
        loadEntry("application/vnd.sun.xml.impress.template", "sti", false);
        loadEntry("application/vnd.sun.xml.math", "sxm", false);
        loadEntry("application/vnd.sun.xml.writer", "sxw", false);
        loadEntry("application/vnd.sun.xml.writer.global", "sxg", false);
        loadEntry("application/vnd.sun.xml.writer.template", "stw", false);
        loadEntry("application/vnd.visio", "vsd", false);
        loadEntry("application/x-abiword", "abw", false);
        loadEntry("application/x-apple-diskimage", "dmg", false);
        loadEntry("application/x-bcpio", "bcpio", false);
        loadEntry("application/x-bittorrent", "torrent", false);
        loadEntry("application/x-cdf", "cdf", false);
        loadEntry("application/x-cdlink", "vcd", false);
        loadEntry("application/x-chess-pgn", "pgn", false);
        loadEntry("application/x-cpio", "cpio", false);
        loadEntry("application/x-debian-package", "deb", false);
        loadEntry("application/x-debian-package", "udeb", false);
        loadEntry("application/x-director", "dcr", false);
        loadEntry("application/x-director", "dir", false);
        loadEntry("application/x-director", "dxr", false);
        loadEntry("application/x-dms", "dms", false);
        loadEntry("application/x-doom", "wad", false);
        loadEntry("application/x-dvi", "dvi", false);
        loadEntry("application/x-flac", "flac", false);
        loadEntry("application/x-font", "pfa", false);
        loadEntry("application/x-font", "pfb", false);
        loadEntry("application/x-font", "gsf", false);
        loadEntry("application/x-font", "pcf", false);
        loadEntry("application/x-font", "pcf.Z", false);
        loadEntry("application/x-freemind", "mm", false);
        loadEntry("application/x-futuresplash", "spl", false);
        loadEntry("application/x-gnumeric", "gnumeric", false);
        loadEntry("application/x-go-sgf", "sgf", false);
        loadEntry("application/x-graphing-calculator", "gcf", false);
        loadEntry("application/x-gtar", "gtar", false);
        loadEntry("application/x-gtar", "tgz", false);
        loadEntry("application/x-gtar", "taz", false);
        loadEntry("application/x-hdf", "hdf", false);
        loadEntry("application/x-ica", "ica", false);
        loadEntry("application/x-internet-signup", "ins", false);
        loadEntry("application/x-internet-signup", "isp", false);
        loadEntry("application/x-iphone", "iii", false);
        loadEntry("application/x-iso9660-image", "iso", false);
        loadEntry("application/x-jmol", "jmz", false);
        loadEntry("application/x-kchart", "chrt", false);
        loadEntry("application/x-killustrator", "kil", false);
        loadEntry("application/x-koan", "skp", false);
        loadEntry("application/x-koan", "skd", false);
        loadEntry("application/x-koan", "skt", false);
        loadEntry("application/x-koan", "skm", false);
        loadEntry("application/x-kpresenter", "kpr", false);
        loadEntry("application/x-kpresenter", "kpt", false);
        loadEntry("application/x-kspread", "ksp", false);
        loadEntry("application/x-kword", "kwd", false);
        loadEntry("application/x-kword", "kwt", false);
        loadEntry("application/x-latex", "latex", false);
        loadEntry("application/x-lha", "lha", false);
        loadEntry("application/x-lzh", "lzh", false);
        loadEntry("application/x-lzx", "lzx", false);
        loadEntry("application/x-maker", "frm", false);
        loadEntry("application/x-maker", "maker", false);
        loadEntry("application/x-maker", "frame", false);
        loadEntry("application/x-maker", "fb", false);
        loadEntry("application/x-maker", "book", false);
        loadEntry("application/x-maker", "fbdoc", false);
        loadEntry("application/x-mif", "mif", false);
        loadEntry("application/x-ms-wmd", "wmd", false);
        loadEntry("application/x-ms-wmz", "wmz", false);
        loadEntry("application/x-msi", "msi", false);
        loadEntry("application/x-ns-proxy-autoconfig", "pac", false);
        loadEntry("application/x-nwc", "nwc", false);
        loadEntry("application/x-object", "o", false);
        loadEntry("application/x-oz-application", "oza", false);
        loadEntry("application/x-pkcs7-certreqresp", "p7r", false);
        loadEntry("application/x-pkcs7-crl", "crl", false);
        loadEntry("application/x-quicktimeplayer", "qtl", false);
        loadEntry("application/x-shar", "shar", false);
        loadEntry("application/x-stuffit", "sit", false);
        loadEntry("application/x-sv4cpio", "sv4cpio", false);
        loadEntry("application/x-sv4crc", "sv4crc", false);
        loadEntry("application/x-tar", "tar", false);
        loadEntry("application/x-texinfo", "texinfo", false);
        loadEntry("application/x-texinfo", "texi", false);
        loadEntry("application/x-troff", "t", false);
        loadEntry("application/x-troff", "roff", false);
        loadEntry("application/x-troff-man", "man", false);
        loadEntry("application/x-ustar", "ustar", false);
        loadEntry("application/x-wais-source", "src", false);
        loadEntry("application/x-wingz", "wz", false);
        loadEntry("application/x-webarchive", "webarchive", false); // added
        loadEntry("application/x-x509-ca-cert", "crt", false);
        loadEntry("application/x-xcf", "xcf", false);
        loadEntry("application/x-xfig", "fig", false);
        loadEntry("audio/basic", "snd", false);
        loadEntry("audio/midi", "mid", false);
        loadEntry("audio/midi", "midi", false);
        loadEntry("audio/midi", "kar", false);
        loadEntry("audio/mpeg", "mpga", false);
        loadEntry("audio/mpeg", "mpega", false);
        loadEntry("audio/mpeg", "mp2", false);
        loadEntry("audio/mpeg", "mp3", false);
        loadEntry("audio/mpeg", "m4a", false);
        loadEntry("audio/mpegurl", "m3u", false);
        loadEntry("audio/prs.sid", "sid", false);
        loadEntry("audio/x-aiff", "aif", false);
        loadEntry("audio/x-aiff", "aiff", false);
        loadEntry("audio/x-aiff", "aifc", false);
        loadEntry("audio/x-gsm", "gsm", false);
        loadEntry("audio/x-mpegurl", "m3u", false);
        loadEntry("audio/x-ms-wma", "wma", false);
        loadEntry("audio/x-ms-wax", "wax", false);
        loadEntry("audio/x-pn-realaudio", "ra", false);
        loadEntry("audio/x-pn-realaudio", "rm", false);
        loadEntry("audio/x-pn-realaudio", "ram", false);
        loadEntry("audio/x-realaudio", "ra", false);
        loadEntry("audio/x-scpls", "pls", false);
        loadEntry("audio/x-sd2", "sd2", false);
        loadEntry("audio/x-wav", "wav", false);
        loadEntry("image/bmp", "bmp", false); // added
        loadEntry("image/gif", "gif", false);
        loadEntry("image/ico", "cur", false); // added
        loadEntry("image/ico", "ico", false); // added
        loadEntry("image/ief", "ief", false);
        loadEntry("image/jpeg", "jpeg", false);
        loadEntry("image/jpeg", "jpg", false);
        loadEntry("image/jpeg", "jpe", false);
        loadEntry("image/pcx", "pcx", false);
        loadEntry("image/png", "png", false);
        loadEntry("image/svg+xml", "svg", false);
        loadEntry("image/svg+xml", "svgz", false);
        loadEntry("image/tiff", "tiff", false);
        loadEntry("image/tiff", "tif", false);
        loadEntry("image/vnd.djvu", "djvu", false);
        loadEntry("image/vnd.djvu", "djv", false);
        loadEntry("image/vnd.wap.wbmp", "wbmp", false);
        loadEntry("image/x-cmu-raster", "ras", false);
        loadEntry("image/x-coreldraw", "cdr", false);
        loadEntry("image/x-coreldrawpattern", "pat", false);
        loadEntry("image/x-coreldrawtemplate", "cdt", false);
        loadEntry("image/x-corelphotopaint", "cpt", false);
        loadEntry("image/x-icon", "ico", false);
        loadEntry("image/x-jg", "art", false);
        loadEntry("image/x-jng", "jng", false);
        loadEntry("image/x-ms-bmp", "bmp", false);
        loadEntry("image/x-photoshop", "psd", false);
        loadEntry("image/x-portable-anymap", "pnm", false);
        loadEntry("image/x-portable-bitmap", "pbm", false);
        loadEntry("image/x-portable-graymap", "pgm", false);
        loadEntry("image/x-portable-pixmap", "ppm", false);
        loadEntry("image/x-rgb", "rgb", false);
        loadEntry("image/x-xbitmap", "xbm", false);
        loadEntry("image/x-xpixmap", "xpm", false);
        loadEntry("image/x-xwindowdump", "xwd", false);
        loadEntry("model/iges", "igs", false);
        loadEntry("model/iges", "iges", false);
        loadEntry("model/mesh", "msh", false);
        loadEntry("model/mesh", "mesh", false);
        loadEntry("model/mesh", "silo", false);
        loadEntry("text/calendar", "ics", true);
        loadEntry("text/calendar", "icz", true);
        loadEntry("text/comma-separated-values", "csv", true);
        loadEntry("text/css", "css", true);
        loadEntry("text/h323", "323", true);
        loadEntry("text/iuls", "uls", true);
        loadEntry("text/mathml", "mml", true);
// add it first so it will be the default for ExtensionFromMimeType
        loadEntry("text/plain", "txt", true);
        loadEntry("text/plain", "asc", true);
        loadEntry("text/plain", "text", true);
        loadEntry("text/plain", "diff", true);
        loadEntry("text/plain", "pot", true);
        loadEntry("text/richtext", "rtx", true);
        loadEntry("text/rtf", "rtf", true);
        loadEntry("text/texmacs", "ts", true);
        loadEntry("text/text", "phps", true);
        loadEntry("text/tab-separated-values", "tsv", true);
        loadEntry("text/x-bibtex", "bib", true);
        loadEntry("text/x-boo", "boo", true);
        loadEntry("text/x-c++hdr", "h++", true);
        loadEntry("text/x-c++hdr", "hpp", true);
        loadEntry("text/x-c++hdr", "hxx", true);
        loadEntry("text/x-c++hdr", "hh", true);
        loadEntry("text/x-c++src", "c++", true);
        loadEntry("text/x-c++src", "cpp", true);
        loadEntry("text/x-c++src", "cxx", true);
        loadEntry("text/x-chdr", "h", true);
        loadEntry("text/x-component", "htc", true);
        loadEntry("text/x-csh", "csh", true);
        loadEntry("text/x-csrc", "c", true);
        loadEntry("text/x-dsrc", "d", true);
        loadEntry("text/x-haskell", "hs", true);
        loadEntry("text/x-java", "java", true);
        loadEntry("text/x-literate-haskell", "lhs", true);
        loadEntry("text/x-moc", "moc", true);
        loadEntry("text/x-pascal", "p", true);
        loadEntry("text/x-pascal", "pas", true);
        loadEntry("text/x-pcs-gcd", "gcd", true);
        loadEntry("text/x-setext", "etx", true);
        loadEntry("text/x-tcl", "tcl", true);
        loadEntry("text/x-tex", "tex", true);
        loadEntry("text/x-tex", "ltx", true);
        loadEntry("text/x-tex", "sty", true);
        loadEntry("text/x-tex", "cls", true);
        loadEntry("text/x-vcalendar", "vcs", true);
        loadEntry("text/x-vcard", "vcf", true);
        loadEntry("video/3gpp", "3gp", false);
        loadEntry("video/3gpp", "3g2", false);
        loadEntry("video/dl", "dl", false);
        loadEntry("video/dv", "dif", false);
        loadEntry("video/dv", "dv", false);
        loadEntry("video/fli", "fli", false);
        loadEntry("video/mpeg", "mpeg", false);
        loadEntry("video/mpeg", "mpg", false);
        loadEntry("video/mpeg", "mpe", false);
        loadEntry("video/mp4", "mp4", false);
        loadEntry("video/mpeg", "VOB", false);
        loadEntry("video/quicktime", "qt", false);
        loadEntry("video/quicktime", "mov", false);
        loadEntry("video/vnd.mpegurl", "mxu", false);
        loadEntry("video/x-la-asf", "lsf", false);
        loadEntry("video/x-la-asf", "lsx", false);
        loadEntry("video/x-mng", "mng", false);
        loadEntry("video/x-ms-asf", "asf", false);
        loadEntry("video/x-ms-asf", "asx", false);
        loadEntry("video/x-ms-wm", "wm", false);
        loadEntry("video/x-ms-wmv", "wmv", false);
        loadEntry("video/x-ms-wmx", "wmx", false);
        loadEntry("video/x-ms-wvx", "wvx", false);
        loadEntry("video/x-msvideo", "avi", false);
        loadEntry("video/x-sgi-movie", "movie", false);
        loadEntry("x-conference/x-cooltalk", "ice", false);
        loadEntry("x-epoc/x-sisx-app", "sisx", false);
    }

    public static class MimeTypeMap {
        private String mMimeType;
        private String mExtension;
        private boolean isTextType;
    }

    private static void loadEntry(String mimeType, String extension, boolean textType) {
        MimeTypeMap map = new MimeTypeMap();
        map.mMimeType = mimeType;
        map.mExtension = extension;
        map.isTextType = textType;
        sMimeTypeMap.put(extension, map);
    }
}
