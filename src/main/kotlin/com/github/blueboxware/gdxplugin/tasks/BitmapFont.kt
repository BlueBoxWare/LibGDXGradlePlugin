package com.github.blueboxware.gdxplugin.tasks

import com.github.blueboxware.BuildConfig
import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.configure
import com.github.blueboxware.gdxplugin.dsl.BitmapFontSettings
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import java.io.File


/*
 * Copyright 2018 Blue Box Ware
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Suppress("MemberVisibilityCanBePrivate")
open class BitmapFont: DefaultTask() {

  @Suppress("unused", "PropertyName")
  @Internal
  val NEHE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*\u007F"

  @Nested
  var settings: BitmapFontSettings = BitmapFontSettings()

  @Input
  var inputFont: Any = "Arial"

  @Internal
  var outputFile: Any? = null
    set(value) {
      field = when (value) {
        is String -> project.file(value)
        is File -> value
        else -> throw GradleException("outputFile: String or File expected")
      }
    }

  @Input
  var characters: String = NEHE

  @Input
  @Optional
  var defaultName: String? = null

  private var outputFonts: MutableList<OutputFontSpec> = mutableListOf()

  init {
    description = "Create a bitmap font using Hiero"
    group = GdxPlugin.TASK_GROUP

    logging.captureStandardOutput(LogLevel.LIFECYCLE)
    logging.captureStandardError(LogLevel.ERROR)
  }

  @TaskAction
  fun generate() {

    if (outputFonts.isEmpty()) {
      throw GradleException("No output size(s) specified")
    }

    val tmpSettingsFile = File(temporaryDir, "font.settings")
    settings.toHieroSettings().apply {
      glyphText = characters
      when (inputFont) {
        is File -> {
          font2File = (inputFont as File).absolutePath
          isFont2Active = true
        }
        is String -> fontName = inputFont as String
        else -> throw GradleException("inputFont should be either a String (name of a system font) or a file (a TTF file)")
      }
    }.save(tmpSettingsFile)

    val cpConfiguration = project.buildscript.configurations.findByName("classpath")?.copy() ?: throw GradleException("Could not find classpath configuration of buildscript")
    val backEndDependency = project.dependencies.create("com.badlogicgames.gdx:gdx-backend-lwjgl:${BuildConfig.GDX_VERSION}")
    val nativesDepedency = project.dependencies.create("com.badlogicgames.gdx:gdx-platform:${BuildConfig.GDX_VERSION}:natives-desktop")
    val ftNativesDependency = project.dependencies.create("com.badlogicgames.gdx:gdx-freetype-platform:${BuildConfig.GDX_VERSION}:natives-desktop")
    cpConfiguration.dependencies.add(backEndDependency)
    cpConfiguration.dependencies.add(nativesDepedency)
    cpConfiguration.dependencies.add(ftNativesDependency)

    project.javaexec { javaExecSpec ->
      javaExecSpec.mainClass.set("com.github.blueboxware.gdxplugin.utils.FontGenerator")
      javaExecSpec.classpath = cpConfiguration
      javaExecSpec.args(
              listOf(tmpSettingsFile.absolutePath) +
              getActualOutputFontSpecs()
                      .map { it.fontSize.toString() + ":" + (it.file?.absolutePath ?: throw AssertionError()) }
      )
    }

  }

  private fun getActualOutputFontSpecs(): List<OutputFontSpec> {
    val baseName = (outputFile as? File)?.absolutePath?.removeSuffix(".fnt") ?: defaultName ?: name
    return outputFonts.map {
      val file = it.file ?: if (outputFonts.size > 1) {
        File(baseName + it.fontSize + "px.fnt")
      } else {
        File("$baseName.fnt")
      }
      OutputFontSpec(it.fontSize, file)
    }
  }

  @OutputFiles
  fun getActualOutputFiles(): List<File> = getActualOutputFontSpecs().mapNotNull { it.file }

  @InputFile
  @Optional
  fun getActualInputFile() = inputFont as? File

  @Input
  fun getInputSizes() = outputFonts.map { it.fontSize }.joinToString()

  @Suppress("unused")
  fun settings(closure: Closure<in BitmapFontSettings>) {
    settings.configure(closure)
  }

  @Suppress("unused")
  fun settings(closure: BitmapFontSettings.() -> Unit) {
    settings.apply(closure)
  }

  data class OutputFontSpec(var fontSize: Int, var file: File? = null)

  @Suppress("unused")
  fun setSize(vararg sizes:Int) = sizes(*sizes)

  @Suppress("unused")
  fun setSizes(vararg sizes: Int) = sizes(*sizes)

  fun size(vararg sizes: Int) = sizes.forEach { outputFonts.add(OutputFontSpec(it)) }

  fun size(size: Int, filename: String) =
          outputFonts.add(OutputFontSpec(size, project.file(filename.removeSuffix(".fnt") + ".fnt")))

  fun size(size: Int, file: File) =
          if (file.extension != "fnt") {
            outputFonts.add(OutputFontSpec(size, File(file.absolutePath + ".fnt")))
          } else {
            outputFonts.add(OutputFontSpec(size, file))
          }

  fun sizes(vararg sizes: Int) = size(*sizes)

  @Suppress("unused", "PropertyName")
  @Internal
  val COMMON = NEHE

  @Suppress("unused", "PropertyName")
  @Internal
  val ASCII = CharRange(32.toChar(), 255.toChar()).joinTo(StringBuilder(), separator = "").toString()

  @Suppress("unused", "PropertyName")
  @Internal
  val EXTENDED = listOf(0, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
            57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86,
            87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113,
            114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170,
            171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194,
            195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218,
            219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242,
            243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266,
            267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290,
            291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314,
            315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338,
            339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362,
            363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 884, 885, 890,
            891, 892, 893, 894, 900, 901, 902, 903, 904, 905, 906, 908, 910, 911, 912, 913, 914, 915, 916, 917, 918, 919, 920, 921,
            922, 923, 924, 925, 926, 927, 928, 929, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943, 944, 945, 946,
            947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970,
            971, 972, 973, 974, 976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995,
            996, 997, 998, 999, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016,
            1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025, 1026, 1027, 1028, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036,
            1037, 1038, 1039, 1040, 1041, 1042, 1043, 1044, 1045, 1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056,
            1057, 1058, 1059, 1060, 1061, 1062, 1063, 1064, 1065, 1066, 1067, 1068, 1069, 1070, 1071, 1072, 1073, 1074, 1075, 1076,
            1077, 1078, 1079, 1080, 1081, 1082, 1083, 1084, 1085, 1086, 1087, 1088, 1089, 1090, 1091, 1092, 1093, 1094, 1095, 1096,
            1097, 1098, 1099, 1100, 1101, 1102, 1103, 1104, 1105, 1106, 1107, 1108, 1109, 1110, 1111, 1112, 1113, 1114, 1115, 1116,
            1117, 1118, 1119, 1120, 1121, 1122, 1123, 1124, 1125, 1126, 1127, 1128, 1129, 1130, 1131, 1132, 1133, 1134, 1135, 1136,
            1137, 1138, 1139, 1140, 1141, 1142, 1143, 1144, 1145, 1146, 1147, 1148, 1149, 1150, 1151, 1152, 1153, 1154, 1155, 1156,
            1157, 1158, 1159, 1160, 1161, 1162, 1163, 1164, 1165, 1166, 1167, 1168, 1169, 1170, 1171, 1172, 1173, 1174, 1175, 1176,
            1177, 1178, 1179, 1180, 1181, 1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193, 1194, 1195, 1196,
            1197, 1198, 1199, 1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212, 1213, 1214, 1215, 1216,
            1217, 1218, 1219, 1220, 1221, 1222, 1223, 1224, 1225, 1226, 1227, 1228, 1229, 1230, 1231, 1232, 1233, 1234, 1235, 1236,
            1237, 1238, 1239, 1240, 1241, 1242, 1243, 1244, 1245, 1246, 1247, 1248, 1249, 1250, 1251, 1252, 1253, 1254, 1255, 1256,
            1257, 1258, 1259, 1260, 1261, 1262, 1263, 1264, 1265, 1266, 1267, 1268, 1269, 1270, 1271, 1272, 1273, 1274, 1275, 1276,
            1277, 1278, 1279, 1280, 1281, 1282, 1283, 1284, 1285, 1286, 1287, 1288, 1289, 1290, 1291, 1292, 1293, 1294, 1295, 1296,
            1297, 1298, 1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1307, 1308, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316,
            1317, 1318, 1319, 8192, 8193, 8194, 8195, 8196, 8197, 8198, 8199, 8200, 8201, 8202, 8203, 8204, 8205, 8206, 8207, 8210,
            8211, 8212, 8213, 8214, 8215, 8216, 8217, 8218, 8219, 8220, 8221, 8222, 8223, 8224, 8225, 8226, 8230, 8234, 8235, 8236,
            8237, 8238, 8239, 8240, 8242, 8243, 8244, 8249, 8250, 8252, 8254, 8260, 8286, 8298, 8299, 8300, 8301, 8302, 8303, 8352,
            8353, 8354, 8355, 8356, 8357, 8358, 8359, 8360, 8361, 8363, 8364, 8365, 8366, 8367, 8368, 8369, 8370, 8371, 8372, 8373,
            8377, 8378, 11360, 11361, 11362, 11363, 11364, 11365, 11366, 11367, 11368, 11369, 11370, 11371, 11372, 11373, 11377,
            11378, 11379, 11380, 11381, 11382, 11383).map { it.toChar() }.joinTo(StringBuilder(), separator = "").toString()

}
