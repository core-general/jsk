package jsk.outer.telegram.mtc.beans.telegram;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.pengrad.telegrambot.request.SendInvoice;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import sk.utils.functional.O;

import java.awt.image.BufferedImage;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.of;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MgcTelegramSpecial {
    O<SendInvoice> payments;
    O<String> sticker;
    O<String> video;
    O<BufferedImage> rawImage;

    public static MgcTelegramSpecial select(O<SendInvoice> payments, O<String> sticker, O<String> video,
            O<BufferedImage> rawImage) {
        return new MgcTelegramSpecial(payments, sticker, video, rawImage);
    }

    public static MgcTelegramSpecial payments(SendInvoice payments) {
        return new MgcTelegramSpecial(of(payments), empty(), empty(), empty());
    }

    public static MgcTelegramSpecial sticker(String sticker) {
        return new MgcTelegramSpecial(empty(), of(sticker), empty(), empty());
    }

    public static MgcTelegramSpecial video(String video) {
        return new MgcTelegramSpecial(empty(), empty(), of(video), empty());
    }

    public static MgcTelegramSpecial rawImage(BufferedImage image) {
        return new MgcTelegramSpecial(empty(), empty(), empty(), of(image));
    }
}
