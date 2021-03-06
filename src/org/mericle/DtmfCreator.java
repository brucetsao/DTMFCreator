package org.mericle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * DTMF生成ツール.
 * @author BladeanMericle
 */
public final class DtmfCreator {

    /** DTMFの周波数テーブル. */
    private static final Hashtable<String, double[]> FREQUENCIES =
        new Hashtable<String, double[]>() { {
            put("1", new double[]{697.0, 1209.0});
            put("2", new double[]{697.0, 1336.0});
            put("3", new double[]{697.0, 1477.0});
            put("A", new double[]{697.0, 1633.0});
            put("4", new double[]{770.0, 1209.0});
            put("5", new double[]{770.0, 1336.0});
            put("6", new double[]{770.0, 1477.0});
            put("B", new double[]{770.0, 1633.0});
            put("7", new double[]{852.0, 1209.0});
            put("8", new double[]{852.0, 1336.0});
            put("9", new double[]{852.0, 1477.0});
            put("C", new double[]{852.0, 1633.0});
            put("S", new double[]{941.0, 1209.0}); // Star "*"
            put("0", new double[]{941.0, 1336.0});
            put("P", new double[]{941.0, 1477.0}); // Pound Sign "#"
            put("D", new double[]{941.0, 1633.0});
        } };

    /** コマンドラインオプションテーブル. */
    private static final Options OPTIONS =
        new Options() { {
            addOption("c", "Correction", true, "周波数補正(%)");
            addOption("l", "Length", true, "音声の長さ(ms)");
            addOption("p", "Path", true, "出力先ファイルパス");
        } };

    /** デフォルトコンストラクタの禁止. */
    private DtmfCreator() { };

    /**
     * @param args コマンドラインオプション
     */
    public static void main(final String[] args) {
        try {
            CommandLineParser parser = new BasicParser();
            CommandLine commandLine = parser.parse(OPTIONS, args);

            float sampleRate = 44000.0f;
            int sampleSize = 8;
            int correction = getCorrection(commandLine);
            long length = getLength(commandLine);
            String path = getPath(commandLine);

            for (Entry<String, double[]> entry : FREQUENCIES.entrySet()) {
                byte[] waveData = new byte[(int) (sampleRate * ((double) length / 1000.0))];
                double[] frequencies = entry.getValue();
                for (int i = 0; i < waveData.length; ++i) {
                    double magnification = ((1 << (sampleSize - 1)) - 1) / frequencies.length;
                    for (int j = 0; j < frequencies.length; ++j) {
                        frequencies[j] = frequencies[j] * ((double) (correction + 100) / 100.0);
                        waveData[i] += (byte) (magnification * Math.sin((double) i * (frequencies[j] * Math.PI * 2.0 / sampleRate)));
                    }
                }
                AudioInputStream inputStream = new AudioInputStream(
                        new ByteArrayInputStream(waveData),
                        new AudioFormat(sampleRate, sampleSize, 1, true, false),
                        waveData.length);
                AudioSystem.write(
                        inputStream,
                        AudioFileFormat.Type.WAVE,
                        new File(path
                                + File.separator
                                + entry.getKey()
                                + "."
                                + AudioFileFormat.Type.WAVE.getExtension()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * コマンドラインから周波数補正値を取得します.
     * @param commandLine コマンドライン
     * @return 周波数補正値
     */
    private static int getCorrection(final CommandLine commandLine) {
        int correction = 0;
        if (commandLine.hasOption("c")) {
            correction = Integer.valueOf(commandLine.getOptionValue("c"));
        }
        return correction;
    }

    /**
     * コマンドラインから音声の長さを取得します.
     * @param commandLine コマンドライン
     * @return 音声の長さ
     */
    private static long getLength(final CommandLine commandLine) {
        long length = 1000;
        if (commandLine.hasOption("l")) {
            length = Long.valueOf(commandLine.getOptionValue("l"));
        }
        return length;
    }

    /**
     * コマンドラインから出力先ファイルパスを取得します.
     * @param commandLine コマンドライン
     * @return 出力先ファイルパス
     */
    private static String getPath(final CommandLine commandLine) {
        String path = "." + File.separator;
        if (commandLine.hasOption("p")) {
            path = commandLine.getOptionValue("p");
            File folder = new File(path);
            if (!folder.isDirectory()) {
                throw new IllegalArgumentException("出力先がディレクトリではありません。");
            }
            // 未存在フォルダの場合は生成してしまう
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
        return path;
    }

}
