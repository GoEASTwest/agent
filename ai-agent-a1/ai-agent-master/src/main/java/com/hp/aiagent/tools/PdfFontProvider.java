package com.hp.aiagent.tools;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PdfFontProvider {

    private static final List<Path> CHINESE_FONT_CANDIDATES = List.of(
            Path.of("C:/Windows/Fonts/msyh.ttc"),
            Path.of("C:/Windows/Fonts/simsun.ttc"),
            Path.of("C:/Windows/Fonts/simhei.ttf"),
            Path.of("C:/Windows/Fonts/NotoSansSC-VF.ttf"),
            Path.of("/System/Library/Fonts/PingFang.ttc"),
            Path.of("/Library/Fonts/Arial Unicode.ttf"),
            Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
            Path.of("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"),
            Path.of("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc")
    );

    private PdfFontProvider() {
    }

    public static PdfFont createChineseFont() throws IOException {
        for (Path fontPath : CHINESE_FONT_CANDIDATES) {
            if (!Files.isRegularFile(fontPath)) {
                continue;
            }
            String normalized = fontPath.toString();
            if (normalized.toLowerCase().endsWith(".ttc")) {
                return PdfFontFactory.createTtcFont(
                        normalized,
                        0,
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED,
                        true
                );
            }
            return PdfFontFactory.createFont(
                    normalized,
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
        }
        return PdfFontFactory.createFont();
    }
}
