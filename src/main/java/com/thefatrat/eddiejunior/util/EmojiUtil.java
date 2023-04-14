package com.thefatrat.eddiejunior.util;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class EmojiUtil {

    @Contract(pure = true)
    public static boolean isEmoji(@NotNull String message) {
        return EMOJIS.contains(message);
    }

    public static Button formatButton(String id, @NotNull String label, ButtonStyle style) {
        String[] split = label.split(" ?\\| ?", 2);

        if (isEmoji(split[0])) {
            Emoji emoji = Emoji.fromUnicode(split[0]);

            if (split.length > 1) {
                return Button.of(style, id, split[1]).withEmoji(emoji);
            } else {
                return Button.of(style, id, emoji);
            }
        } else {
            return Button.of(style, id, label);
        }
    }

    public static final Set<String> EMOJIS = Set.of(
        "😀",
        "😃",
        "😄",
        "😁",
        "😆",
        "😅",
        "🤣",
        "😂",
        "🙂",
        "🙃",
        "🫠",
        "😉",
        "😊",
        "😇",
        "🥰",
        "😍",
        "🤩",
        "😘",
        "😗",
        "☺️",
        "😚",
        "😙",
        "🥲",
        "😋",
        "😛",
        "😜",
        "🤪",
        "😝",
        "🤑",
        "🤗",
        "🤭",
        "🫢",
        "🫣",
        "🤫",
        "🤔",
        "🫡",
        "🤐",
        "🤨",
        "😐",
        "😑",
        "😶",
        "🫥",
        "😶‍🌫️",
        "😏",
        "😒",
        "🙄",
        "😬",
        "😮‍💨",
        "🤥",
        "🫨",
        "😌",
        "😔",
        "😪",
        "🤤",
        "😴",
        "😷",
        "🤒",
        "🤕",
        "🤢",
        "🤮",
        "🤧",
        "🥵",
        "🥶",
        "🥴",
        "😵",
        "😵‍💫",
        "🤯",
        "🤠",
        "🥳",
        "🥸",
        "😎",
        "🤓",
        "🧐",
        "😕",
        "🫤",
        "😟",
        "🙁",
        "☹️",
        "😮",
        "😯",
        "😲",
        "😳",
        "🥺",
        "🥹",
        "😦",
        "😧",
        "😨",
        "😰",
        "😥",
        "😢",
        "😭",
        "😱",
        "😖",
        "😣",
        "😞",
        "😓",
        "😩",
        "😫",
        "🥱",
        "😤",
        "😡",
        "😠",
        "🤬",
        "😈",
        "👿",
        "💀",
        "☠️",
        "💩",
        "🤡",
        "👹",
        "👺",
        "👻",
        "👽",
        "👾",
        "🤖",
        "😺",
        "😸",
        "😹",
        "😻",
        "😼",
        "😽",
        "🙀",
        "😿",
        "😾",
        "🙈",
        "🙉",
        "🙊",
        "💌",
        "💘",
        "💝",
        "💖",
        "💗",
        "💓",
        "💞",
        "💕",
        "💟",
        "❣️",
        "💔",
        "❤️‍🔥",
        "❤️‍🩹",
        "❤️",
        "🩷",
        "🧡",
        "💛",
        "💚",
        "💙",
        "🩵",
        "💜",
        "🤎",
        "🖤",
        "🩶",
        "🤍",
        "💋",
        "💯",
        "💢",
        "💥",
        "💫",
        "💦",
        "💨",
        "🕳️",
        "💬",
        "👁️‍🗨️",
        "🗨️",
        "🗯️",
        "💭",
        "💤",
        "👋",
        "🤚",
        "🖐️",
        "✋",
        "🖖",
        "🫱",
        "🫲",
        "🫳",
        "🫴",
        "🫷",
        "🫸",
        "👌",
        "🤌",
        "🤏",
        "✌️",
        "🤞",
        "🫰",
        "🤟",
        "🤘",
        "🤙",
        "👈",
        "👉",
        "👆",
        "🖕",
        "👇",
        "☝️",
        "🫵",
        "👍",
        "👎",
        "✊",
        "👊",
        "🤛",
        "🤜",
        "👏",
        "🙌",
        "🫶",
        "👐",
        "🤲",
        "🤝",
        "🙏",
        "✍️",
        "💅",
        "🤳",
        "💪",
        "🦾",
        "🦿",
        "🦵",
        "🦶",
        "👂",
        "🦻",
        "👃",
        "🧠",
        "🫀",
        "🫁",
        "🦷",
        "🦴",
        "👀",
        "👁️",
        "👅",
        "👄",
        "🫦",
        "👶",
        "🧒",
        "👦",
        "👧",
        "🧑",
        "👱",
        "👨",
        "🧔",
        "🧔‍♂️",
        "🧔‍♀️",
        "👨‍🦰",
        "👨‍🦱",
        "👨‍🦳",
        "👨‍🦲",
        "👩",
        "👩‍🦰",
        "🧑‍🦰",
        "👩‍🦱",
        "🧑‍🦱",
        "👩‍🦳",
        "🧑‍🦳",
        "👩‍🦲",
        "🧑‍🦲",
        "👱‍♀️",
        "👱‍♂️",
        "🧓",
        "👴",
        "👵",
        "🙍",
        "🙍‍♂️",
        "🙍‍♀️",
        "🙎",
        "🙎‍♂️",
        "🙎‍♀️",
        "🙅",
        "🙅‍♂️",
        "🙅‍♀️",
        "🙆",
        "🙆‍♂️",
        "🙆‍♀️",
        "💁",
        "💁‍♂️",
        "💁‍♀️",
        "🙋",
        "🙋‍♂️",
        "🙋‍♀️",
        "🧏",
        "🧏‍♂️",
        "🧏‍♀️",
        "🙇",
        "🙇‍♂️",
        "🙇‍♀️",
        "🤦",
        "🤦‍♂️",
        "🤦‍♀️",
        "🤷",
        "🤷‍♂️",
        "🤷‍♀️",
        "🧑‍⚕️",
        "👨‍⚕️",
        "👩‍⚕️",
        "🧑‍🎓",
        "👨‍🎓",
        "👩‍🎓",
        "🧑‍🏫",
        "👨‍🏫",
        "👩‍🏫",
        "🧑‍⚖️",
        "👨‍⚖️",
        "👩‍⚖️",
        "🧑‍🌾",
        "👨‍🌾",
        "👩‍🌾",
        "🧑‍🍳",
        "👨‍🍳",
        "👩‍🍳",
        "🧑‍🔧",
        "👨‍🔧",
        "👩‍🔧",
        "🧑‍🏭",
        "👨‍🏭",
        "👩‍🏭",
        "🧑‍💼",
        "👨‍💼",
        "👩‍💼",
        "🧑‍🔬",
        "👨‍🔬",
        "👩‍🔬",
        "🧑‍💻",
        "👨‍💻",
        "👩‍💻",
        "🧑‍🎤",
        "👨‍🎤",
        "👩‍🎤",
        "🧑‍🎨",
        "👨‍🎨",
        "👩‍🎨",
        "🧑‍✈️",
        "👨‍✈️",
        "👩‍✈️",
        "🧑‍🚀",
        "👨‍🚀",
        "👩‍🚀",
        "🧑‍🚒",
        "👨‍🚒",
        "👩‍🚒",
        "👮",
        "👮‍♂️",
        "👮‍♀️",
        "🕵️",
        "🕵️‍♂️",
        "🕵️‍♀️",
        "💂",
        "💂‍♂️",
        "💂‍♀️",
        "🥷",
        "👷",
        "👷‍♂️",
        "👷‍♀️",
        "🫅",
        "🤴",
        "👸",
        "👳",
        "👳‍♂️",
        "👳‍♀️",
        "👲",
        "🧕",
        "🤵",
        "🤵‍♂️",
        "🤵‍♀️",
        "👰",
        "👰‍♂️",
        "👰‍♀️",
        "🤰",
        "🫃",
        "🫄",
        "🤱",
        "👩‍🍼",
        "👨‍🍼",
        "🧑‍🍼",
        "👼",
        "🎅",
        "🤶",
        "🧑‍🎄",
        "🦸",
        "🦸‍♂️",
        "🦸‍♀️",
        "🦹",
        "🦹‍♂️",
        "🦹‍♀️",
        "🧙",
        "🧙‍♂️",
        "🧙‍♀️",
        "🧚",
        "🧚‍♂️",
        "🧚‍♀️",
        "🧛",
        "🧛‍♂️",
        "🧛‍♀️",
        "🧜",
        "🧜‍♂️",
        "🧜‍♀️",
        "🧝",
        "🧝‍♂️",
        "🧝‍♀️",
        "🧞",
        "🧞‍♂️",
        "🧞‍♀️",
        "🧟",
        "🧟‍♂️",
        "🧟‍♀️",
        "🧌",
        "💆",
        "💆‍♂️",
        "💆‍♀️",
        "💇",
        "💇‍♂️",
        "💇‍♀️",
        "🚶",
        "🚶‍♂️",
        "🚶‍♀️",
        "🧍",
        "🧍‍♂️",
        "🧍‍♀️",
        "🧎",
        "🧎‍♂️",
        "🧎‍♀️",
        "🧑‍🦯",
        "👨‍🦯",
        "👩‍🦯",
        "🧑‍🦼",
        "👨‍🦼",
        "👩‍🦼",
        "🧑‍🦽",
        "👨‍🦽",
        "👩‍🦽",
        "🏃",
        "🏃‍♂️",
        "🏃‍♀️",
        "💃",
        "🕺",
        "🕴️",
        "👯",
        "👯‍♂️",
        "👯‍♀️",
        "🧖",
        "🧖‍♂️",
        "🧖‍♀️",
        "🧗",
        "🧗‍♂️",
        "🧗‍♀️",
        "🤺",
        "🏇",
        "⛷️",
        "🏂",
        "🏌️",
        "🏌️‍♂️",
        "🏌️‍♀️",
        "🏄",
        "🏄‍♂️",
        "🏄‍♀️",
        "🚣",
        "🚣‍♂️",
        "🚣‍♀️",
        "🏊",
        "🏊‍♂️",
        "🏊‍♀️",
        "⛹️",
        "⛹️‍♂️",
        "⛹️‍♀️",
        "🏋️",
        "🏋️‍♂️",
        "🏋️‍♀️",
        "🚴",
        "🚴‍♂️",
        "🚴‍♀️",
        "🚵",
        "🚵‍♂️",
        "🚵‍♀️",
        "🤸",
        "🤸‍♂️",
        "🤸‍♀️",
        "🤼",
        "🤼‍♂️",
        "🤼‍♀️",
        "🤽",
        "🤽‍♂️",
        "🤽‍♀️",
        "🤾",
        "🤾‍♂️",
        "🤾‍♀️",
        "🤹",
        "🤹‍♂️",
        "🤹‍♀️",
        "🧘",
        "🧘‍♂️",
        "🧘‍♀️",
        "🛀",
        "🛌",
        "🧑‍🤝‍🧑",
        "👭",
        "👫",
        "👬",
        "💏",
        "👩‍❤️‍💋‍👨",
        "👨‍❤️‍💋‍👨",
        "👩‍❤️‍💋‍👩",
        "💑",
        "👩‍❤️‍👨",
        "👨‍❤️‍👨",
        "👩‍❤️‍👩",
        "👪",
        "👨‍👩‍👦",
        "👨‍👩‍👧",
        "👨‍👩‍👧‍👦",
        "👨‍👩‍👦‍👦",
        "👨‍👩‍👧‍👧",
        "👨‍👨‍👦",
        "👨‍👨‍👧",
        "👨‍👨‍👧‍👦",
        "👨‍👨‍👦‍👦",
        "👨‍👨‍👧‍👧",
        "👩‍👩‍👦",
        "👩‍👩‍👧",
        "👩‍👩‍👧‍👦",
        "👩‍👩‍👦‍👦",
        "👩‍👩‍👧‍👧",
        "👨‍👦",
        "👨‍👦‍👦",
        "👨‍👧",
        "👨‍👧‍👦",
        "👨‍👧‍👧",
        "👩‍👦",
        "👩‍👦‍👦",
        "👩‍👧",
        "👩‍👧‍👦",
        "👩‍👧‍👧",
        "🗣️",
        "👤",
        "👥",
        "🫂",
        "👣",
        "🐵",
        "🐒",
        "🦍",
        "🦧",
        "🐶",
        "🐕",
        "🦮",
        "🐕‍🦺",
        "🐩",
        "🐺",
        "🦊",
        "🦝",
        "🐱",
        "🐈",
        "🐈‍⬛",
        "🦁",
        "🐯",
        "🐅",
        "🐆",
        "🐴",
        "🫎",
        "🫏",
        "🐎",
        "🦄",
        "🦓",
        "🦌",
        "🦬",
        "🐮",
        "🐂",
        "🐃",
        "🐄",
        "🐷",
        "🐖",
        "🐗",
        "🐽",
        "🐏",
        "🐑",
        "🐐",
        "🐪",
        "🐫",
        "🦙",
        "🦒",
        "🐘",
        "🦣",
        "🦏",
        "🦛",
        "🐭",
        "🐁",
        "🐀",
        "🐹",
        "🐰",
        "🐇",
        "🐿️",
        "🦫",
        "🦔",
        "🦇",
        "🐻",
        "🐻‍❄️",
        "🐨",
        "🐼",
        "🦥",
        "🦦",
        "🦨",
        "🦘",
        "🦡",
        "🐾",
        "🦃",
        "🐔",
        "🐓",
        "🐣",
        "🐤",
        "🐥",
        "🐦",
        "🐧",
        "🕊️",
        "🦅",
        "🦆",
        "🦢",
        "🦉",
        "🦤",
        "🪶",
        "🦩",
        "🦚",
        "🦜",
        "🪽",
        "🐦‍⬛",
        "🪿",
        "🐸",
        "🐊",
        "🐢",
        "🦎",
        "🐍",
        "🐲",
        "🐉",
        "🦕",
        "🦖",
        "🐳",
        "🐋",
        "🐬",
        "🦭",
        "🐟",
        "🐠",
        "🐡",
        "🦈",
        "🐙",
        "🐚",
        "🪸",
        "🪼",
        "🐌",
        "🦋",
        "🐛",
        "🐜",
        "🐝",
        "🪲",
        "🐞",
        "🦗",
        "🪳",
        "🕷️",
        "🕸️",
        "🦂",
        "🦟",
        "🪰",
        "🪱",
        "🦠",
        "💐",
        "🌸",
        "💮",
        "🪷",
        "🏵️",
        "🌹",
        "🥀",
        "🌺",
        "🌻",
        "🌼",
        "🌷",
        "🪻",
        "🌱",
        "🪴",
        "🌲",
        "🌳",
        "🌴",
        "🌵",
        "🌾",
        "🌿",
        "☘️",
        "🍀",
        "🍁",
        "🍂",
        "🍃",
        "🪹",
        "🪺",
        "🍄",
        "🍇",
        "🍈",
        "🍉",
        "🍊",
        "🍋",
        "🍌",
        "🍍",
        "🥭",
        "🍎",
        "🍏",
        "🍐",
        "🍑",
        "🍒",
        "🍓",
        "🫐",
        "🥝",
        "🍅",
        "🫒",
        "🥥",
        "🥑",
        "🍆",
        "🥔",
        "🥕",
        "🌽",
        "🌶️",
        "🫑",
        "🥒",
        "🥬",
        "🥦",
        "🧄",
        "🧅",
        "🥜",
        "🫘",
        "🌰",
        "🫚",
        "🫛",
        "🍞",
        "🥐",
        "🥖",
        "🫓",
        "🥨",
        "🥯",
        "🥞",
        "🧇",
        "🧀",
        "🍖",
        "🍗",
        "🥩",
        "🥓",
        "🍔",
        "🍟",
        "🍕",
        "🌭",
        "🥪",
        "🌮",
        "🌯",
        "🫔",
        "🥙",
        "🧆",
        "🥚",
        "🍳",
        "🥘",
        "🍲",
        "🫕",
        "🥣",
        "🥗",
        "🍿",
        "🧈",
        "🧂",
        "🥫",
        "🍱",
        "🍘",
        "🍙",
        "🍚",
        "🍛",
        "🍜",
        "🍝",
        "🍠",
        "🍢",
        "🍣",
        "🍤",
        "🍥",
        "🥮",
        "🍡",
        "🥟",
        "🥠",
        "🥡",
        "🦀",
        "🦞",
        "🦐",
        "🦑",
        "🦪",
        "🍦",
        "🍧",
        "🍨",
        "🍩",
        "🍪",
        "🎂",
        "🍰",
        "🧁",
        "🥧",
        "🍫",
        "🍬",
        "🍭",
        "🍮",
        "🍯",
        "🍼",
        "🥛",
        "☕",
        "🫖",
        "🍵",
        "🍶",
        "🍾",
        "🍷",
        "🍸",
        "🍹",
        "🍺",
        "🍻",
        "🥂",
        "🥃",
        "🫗",
        "🥤",
        "🧋",
        "🧃",
        "🧉",
        "🧊",
        "🥢",
        "🍽️",
        "🍴",
        "🥄",
        "🔪",
        "🫙",
        "🏺",
        "🌍",
        "🌎",
        "🌏",
        "🌐",
        "🗺️",
        "🗾",
        "🧭",
        "🏔️",
        "⛰️",
        "🌋",
        "🗻",
        "🏕️",
        "🏖️",
        "🏜️",
        "🏝️",
        "🏞️",
        "🏟️",
        "🏛️",
        "🏗️",
        "🧱",
        "🪨",
        "🪵",
        "🛖",
        "🏘️",
        "🏚️",
        "🏠",
        "🏡",
        "🏢",
        "🏣",
        "🏤",
        "🏥",
        "🏦",
        "🏨",
        "🏩",
        "🏪",
        "🏫",
        "🏬",
        "🏭",
        "🏯",
        "🏰",
        "💒",
        "🗼",
        "🗽",
        "⛪",
        "🕌",
        "🛕",
        "🕍",
        "⛩️",
        "🕋",
        "⛲",
        "⛺",
        "🌁",
        "🌃",
        "🏙️",
        "🌄",
        "🌅",
        "🌆",
        "🌇",
        "🌉",
        "♨️",
        "🎠",
        "🛝",
        "🎡",
        "🎢",
        "💈",
        "🎪",
        "🚂",
        "🚃",
        "🚄",
        "🚅",
        "🚆",
        "🚇",
        "🚈",
        "🚉",
        "🚊",
        "🚝",
        "🚞",
        "🚋",
        "🚌",
        "🚍",
        "🚎",
        "🚐",
        "🚑",
        "🚒",
        "🚓",
        "🚔",
        "🚕",
        "🚖",
        "🚗",
        "🚘",
        "🚙",
        "🛻",
        "🚚",
        "🚛",
        "🚜",
        "🏎️",
        "🏍️",
        "🛵",
        "🦽",
        "🦼",
        "🛺",
        "🚲",
        "🛴",
        "🛹",
        "🛼",
        "🚏",
        "🛣️",
        "🛤️",
        "🛢️",
        "⛽",
        "🛞",
        "🚨",
        "🚥",
        "🚦",
        "🛑",
        "🚧",
        "⚓",
        "🛟",
        "⛵",
        "🛶",
        "🚤",
        "🛳️",
        "⛴️",
        "🛥️",
        "🚢",
        "✈️",
        "🛩️",
        "🛫",
        "🛬",
        "🪂",
        "💺",
        "🚁",
        "🚟",
        "🚠",
        "🚡",
        "🛰️",
        "🚀",
        "🛸",
        "🛎️",
        "🧳",
        "⌛",
        "⏳",
        "⌚",
        "⏰",
        "⏱️",
        "⏲️",
        "🕰️",
        "🕛",
        "🕧",
        "🕐",
        "🕜",
        "🕑",
        "🕝",
        "🕒",
        "🕞",
        "🕓",
        "🕟",
        "🕔",
        "🕠",
        "🕕",
        "🕡",
        "🕖",
        "🕢",
        "🕗",
        "🕣",
        "🕘",
        "🕤",
        "🕙",
        "🕥",
        "🕚",
        "🕦",
        "🌑",
        "🌒",
        "🌓",
        "🌔",
        "🌕",
        "🌖",
        "🌗",
        "🌘",
        "🌙",
        "🌚",
        "🌛",
        "🌜",
        "🌡️",
        "☀️",
        "🌝",
        "🌞",
        "🪐",
        "⭐",
        "🌟",
        "🌠",
        "🌌",
        "☁️",
        "⛅",
        "⛈️",
        "🌤️",
        "🌥️",
        "🌦️",
        "🌧️",
        "🌨️",
        "🌩️",
        "🌪️",
        "🌫️",
        "🌬️",
        "🌀",
        "🌈",
        "🌂",
        "☂️",
        "☔",
        "⛱️",
        "⚡",
        "❄️",
        "☃️",
        "⛄",
        "☄️",
        "🔥",
        "💧",
        "🌊",
        "🎃",
        "🎄",
        "🎆",
        "🎇",
        "🧨",
        "✨",
        "🎈",
        "🎉",
        "🎊",
        "🎋",
        "🎍",
        "🎎",
        "🎏",
        "🎐",
        "🎑",
        "🧧",
        "🎀",
        "🎁",
        "🎗️",
        "🎟️",
        "🎫",
        "🎖️",
        "🏆",
        "🏅",
        "🥇",
        "🥈",
        "🥉",
        "⚽",
        "⚾",
        "🥎",
        "🏀",
        "🏐",
        "🏈",
        "🏉",
        "🎾",
        "🥏",
        "🎳",
        "🏏",
        "🏑",
        "🏒",
        "🥍",
        "🏓",
        "🏸",
        "🥊",
        "🥋",
        "🥅",
        "⛳",
        "⛸️",
        "🎣",
        "🤿",
        "🎽",
        "🎿",
        "🛷",
        "🥌",
        "🎯",
        "🪀",
        "🪁",
        "🔫",
        "🎱",
        "🔮",
        "🪄",
        "🎮",
        "🕹️",
        "🎰",
        "🎲",
        "🧩",
        "🧸",
        "🪅",
        "🪩",
        "🪆",
        "♠️",
        "♥️",
        "♦️",
        "♣️",
        "♟️",
        "🃏",
        "🀄",
        "🎴",
        "🎭",
        "🖼️",
        "🎨",
        "🧵",
        "🪡",
        "🧶",
        "🪢",
        "👓",
        "🕶️",
        "🥽",
        "🥼",
        "🦺",
        "👔",
        "👕",
        "👖",
        "🧣",
        "🧤",
        "🧥",
        "🧦",
        "👗",
        "👘",
        "🥻",
        "🩱",
        "🩲",
        "🩳",
        "👙",
        "👚",
        "🪭",
        "👛",
        "👜",
        "👝",
        "🛍️",
        "🎒",
        "🩴",
        "👞",
        "👟",
        "🥾",
        "🥿",
        "👠",
        "👡",
        "🩰",
        "👢",
        "🪮",
        "👑",
        "👒",
        "🎩",
        "🎓",
        "🧢",
        "🪖",
        "⛑️",
        "📿",
        "💄",
        "💍",
        "💎",
        "🔇",
        "🔈",
        "🔉",
        "🔊",
        "📢",
        "📣",
        "📯",
        "🔔",
        "🔕",
        "🎼",
        "🎵",
        "🎶",
        "🎙️",
        "🎚️",
        "🎛️",
        "🎤",
        "🎧",
        "📻",
        "🎷",
        "🪗",
        "🎸",
        "🎹",
        "🎺",
        "🎻",
        "🪕",
        "🥁",
        "🪘",
        "🪇",
        "🪈",
        "📱",
        "📲",
        "☎️",
        "📞",
        "📟",
        "📠",
        "🔋",
        "🪫",
        "🔌",
        "💻",
        "🖥️",
        "🖨️",
        "⌨️",
        "🖱️",
        "🖲️",
        "💽",
        "💾",
        "💿",
        "📀",
        "🧮",
        "🎥",
        "🎞️",
        "📽️",
        "🎬",
        "📺",
        "📷",
        "📸",
        "📹",
        "📼",
        "🔍",
        "🔎",
        "🕯️",
        "💡",
        "🔦",
        "🏮",
        "🪔",
        "📔",
        "📕",
        "📖",
        "📗",
        "📘",
        "📙",
        "📚",
        "📓",
        "📒",
        "📃",
        "📜",
        "📄",
        "📰",
        "🗞️",
        "📑",
        "🔖",
        "🏷️",
        "💰",
        "🪙",
        "💴",
        "💵",
        "💶",
        "💷",
        "💸",
        "💳",
        "🧾",
        "💹",
        "✉️",
        "📧",
        "📨",
        "📩",
        "📤",
        "📥",
        "📦",
        "📫",
        "📪",
        "📬",
        "📭",
        "📮",
        "🗳️",
        "✏️",
        "✒️",
        "🖋️",
        "🖊️",
        "🖌️",
        "🖍️",
        "📝",
        "💼",
        "📁",
        "📂",
        "🗂️",
        "📅",
        "📆",
        "🗒️",
        "🗓️",
        "📇",
        "📈",
        "📉",
        "📊",
        "📋",
        "📌",
        "📍",
        "📎",
        "🖇️",
        "📏",
        "📐",
        "✂️",
        "🗃️",
        "🗄️",
        "🗑️",
        "🔒",
        "🔓",
        "🔏",
        "🔐",
        "🔑",
        "🗝️",
        "🔨",
        "🪓",
        "⛏️",
        "⚒️",
        "🛠️",
        "🗡️",
        "⚔️",
        "💣",
        "🪃",
        "🏹",
        "🛡️",
        "🪚",
        "🔧",
        "🪛",
        "🔩",
        "⚙️",
        "🗜️",
        "⚖️",
        "🦯",
        "🔗",
        "⛓️",
        "🪝",
        "🧰",
        "🧲",
        "🪜",
        "⚗️",
        "🧪",
        "🧫",
        "🧬",
        "🔬",
        "🔭",
        "📡",
        "💉",
        "🩸",
        "💊",
        "🩹",
        "🩼",
        "🩺",
        "🩻",
        "🚪",
        "🛗",
        "🪞",
        "🪟",
        "🛏️",
        "🛋️",
        "🪑",
        "🚽",
        "🪠",
        "🚿",
        "🛁",
        "🪤",
        "🪒",
        "🧴",
        "🧷",
        "🧹",
        "🧺",
        "🧻",
        "🪣",
        "🧼",
        "🫧",
        "🪥",
        "🧽",
        "🧯",
        "🛒",
        "🚬",
        "⚰️",
        "🪦",
        "⚱️",
        "🧿",
        "🪬",
        "🗿",
        "🪧",
        "🪪",
        "🏧",
        "🚮",
        "🚰",
        "♿",
        "🚹",
        "🚺",
        "🚻",
        "🚼",
        "🚾",
        "🛂",
        "🛃",
        "🛄",
        "🛅",
        "⚠️",
        "🚸",
        "⛔",
        "🚫",
        "🚳",
        "🚭",
        "🚯",
        "🚱",
        "🚷",
        "📵",
        "🔞",
        "☢️",
        "☣️",
        "⬆️",
        "↗️",
        "➡️",
        "↘️",
        "⬇️",
        "↙️",
        "⬅️",
        "↖️",
        "↕️",
        "↔️",
        "↩️",
        "↪️",
        "⤴️",
        "⤵️",
        "🔃",
        "🔄",
        "🔙",
        "🔚",
        "🔛",
        "🔜",
        "🔝",
        "🛐",
        "⚛️",
        "🕉️",
        "✡️",
        "☸️",
        "☯️",
        "✝️",
        "☦️",
        "☪️",
        "☮️",
        "🕎",
        "🔯",
        "🪯",
        "♈",
        "♉",
        "♊",
        "♋",
        "♌",
        "♍",
        "♎",
        "♏",
        "♐",
        "♑",
        "♒",
        "♓",
        "⛎",
        "🔀",
        "🔁",
        "🔂",
        "▶️",
        "⏩",
        "⏭️",
        "⏯️",
        "◀️",
        "⏪",
        "⏮️",
        "🔼",
        "⏫",
        "🔽",
        "⏬",
        "⏸️",
        "⏹️",
        "⏺️",
        "⏏️",
        "🎦",
        "🔅",
        "🔆",
        "📶",
        "🛜",
        "📳",
        "📴",
        "♀️",
        "♂️",
        "⚧️",
        "✖️",
        "➕",
        "➖",
        "➗",
        "🟰",
        "♾️",
        "‼️",
        "⁉️",
        "❓",
        "❔",
        "❕",
        "❗",
        "〰️",
        "💱",
        "💲",
        "⚕️",
        "♻️",
        "⚜️",
        "🔱",
        "📛",
        "🔰",
        "⭕",
        "✅",
        "☑️",
        "✔️",
        "❌",
        "❎",
        "➰",
        "➿",
        "〽️",
        "✳️",
        "✴️",
        "❇️",
        "©️",
        "®️",
        "™️",
        "#️⃣",
        "*️⃣",
        "0️⃣",
        "1️⃣",
        "2️⃣",
        "3️⃣",
        "4️⃣",
        "5️⃣",
        "6️⃣",
        "7️⃣",
        "8️⃣",
        "9️⃣",
        "🔟",
        "🔠",
        "🔡",
        "🔢",
        "🔣",
        "🔤",
        "🅰️",
        "🆎",
        "🅱️",
        "🆑",
        "🆒",
        "🆓",
        "ℹ️",
        "🆔",
        "Ⓜ️",
        "🆕",
        "🆖",
        "🅾️",
        "🆗",
        "🅿️",
        "🆘",
        "🆙",
        "🆚",
        "🈁",
        "🈂️",
        "🈷️",
        "🈶",
        "🈯",
        "🉐",
        "🈹",
        "🈚",
        "🈲",
        "🉑",
        "🈸",
        "🈴",
        "🈳",
        "㊗️",
        "㊙️",
        "🈺",
        "🈵",
        "🔴",
        "🟠",
        "🟡",
        "🟢",
        "🔵",
        "🟣",
        "🟤",
        "⚫",
        "⚪",
        "🟥",
        "🟧",
        "🟨",
        "🟩",
        "🟦",
        "🟪",
        "🟫",
        "⬛",
        "⬜",
        "◼️",
        "◻️",
        "◾",
        "◽",
        "▪️",
        "▫️",
        "🔶",
        "🔷",
        "🔸",
        "🔹",
        "🔺",
        "🔻",
        "💠",
        "🔘",
        "🔳",
        "🔲",
        "🏁",
        "🚩",
        "🎌",
        "🏴",
        "🏳️",
        "🏳️‍🌈",
        "🏳️‍⚧️",
        "🏴‍☠️",
        "🇦🇨",
        "🇦🇩",
        "🇦🇪",
        "🇦🇫",
        "🇦🇬",
        "🇦🇮",
        "🇦🇱",
        "🇦🇲",
        "🇦🇴",
        "🇦🇶",
        "🇦🇷",
        "🇦🇸",
        "🇦🇹",
        "🇦🇺",
        "🇦🇼",
        "🇦🇽",
        "🇦🇿",
        "🇧🇦",
        "🇧🇧",
        "🇧🇩",
        "🇧🇪",
        "🇧🇫",
        "🇧🇬",
        "🇧🇭",
        "🇧🇮",
        "🇧🇯",
        "🇧🇱",
        "🇧🇲",
        "🇧🇳",
        "🇧🇴",
        "🇧🇶",
        "🇧🇷",
        "🇧🇸",
        "🇧🇹",
        "🇧🇻",
        "🇧🇼",
        "🇧🇾",
        "🇧🇿",
        "🇨🇦",
        "🇨🇨",
        "🇨🇩",
        "🇨🇫",
        "🇨🇬",
        "🇨🇭",
        "🇨🇮",
        "🇨🇰",
        "🇨🇱",
        "🇨🇲",
        "🇨🇳",
        "🇨🇴",
        "🇨🇵",
        "🇨🇷",
        "🇨🇺",
        "🇨🇻",
        "🇨🇼",
        "🇨🇽",
        "🇨🇾",
        "🇨🇿",
        "🇩🇪",
        "🇩🇬",
        "🇩🇯",
        "🇩🇰",
        "🇩🇲",
        "🇩🇴",
        "🇩🇿",
        "🇪🇦",
        "🇪🇨",
        "🇪🇪",
        "🇪🇬",
        "🇪🇭",
        "🇪🇷",
        "🇪🇸",
        "🇪🇹",
        "🇪🇺",
        "🇫🇮",
        "🇫🇯",
        "🇫🇰",
        "🇫🇲",
        "🇫🇴",
        "🇫🇷",
        "🇬🇦",
        "🇬🇧",
        "🇬🇩",
        "🇬🇪",
        "🇬🇫",
        "🇬🇬",
        "🇬🇭",
        "🇬🇮",
        "🇬🇱",
        "🇬🇲",
        "🇬🇳",
        "🇬🇵",
        "🇬🇶",
        "🇬🇷",
        "🇬🇸",
        "🇬🇹",
        "🇬🇺",
        "🇬🇼",
        "🇬🇾",
        "🇭🇰",
        "🇭🇲",
        "🇭🇳",
        "🇭🇷",
        "🇭🇹",
        "🇭🇺",
        "🇮🇨",
        "🇮🇩",
        "🇮🇪",
        "🇮🇱",
        "🇮🇲",
        "🇮🇳",
        "🇮🇴",
        "🇮🇶",
        "🇮🇷",
        "🇮🇸",
        "🇮🇹",
        "🇯🇪",
        "🇯🇲",
        "🇯🇴",
        "🇯🇵",
        "🇰🇪",
        "🇰🇬",
        "🇰🇭",
        "🇰🇮",
        "🇰🇲",
        "🇰🇳",
        "🇰🇵",
        "🇰🇷",
        "🇰🇼",
        "🇰🇾",
        "🇰🇿",
        "🇱🇦",
        "🇱🇧",
        "🇱🇨",
        "🇱🇮",
        "🇱🇰",
        "🇱🇷",
        "🇱🇸",
        "🇱🇹",
        "🇱🇺",
        "🇱🇻",
        "🇱🇾",
        "🇲🇦",
        "🇲🇨",
        "🇲🇩",
        "🇲🇪",
        "🇲🇫",
        "🇲🇬",
        "🇲🇭",
        "🇲🇰",
        "🇲🇱",
        "🇲🇲",
        "🇲🇳",
        "🇲🇴",
        "🇲🇵",
        "🇲🇶",
        "🇲🇷",
        "🇲🇸",
        "🇲🇹",
        "🇲🇺",
        "🇲🇻",
        "🇲🇼",
        "🇲🇽",
        "🇲🇾",
        "🇲🇿",
        "🇳🇦",
        "🇳🇨",
        "🇳🇪",
        "🇳🇫",
        "🇳🇬",
        "🇳🇮",
        "🇳🇱",
        "🇳🇴",
        "🇳🇵",
        "🇳🇷",
        "🇳🇺",
        "🇳🇿",
        "🇴🇲",
        "🇵🇦",
        "🇵🇪",
        "🇵🇫",
        "🇵🇬",
        "🇵🇭",
        "🇵🇰",
        "🇵🇱",
        "🇵🇲",
        "🇵🇳",
        "🇵🇷",
        "🇵🇸",
        "🇵🇹",
        "🇵🇼",
        "🇵🇾",
        "🇶🇦",
        "🇷🇪",
        "🇷🇴",
        "🇷🇸",
        "🇷🇺",
        "🇷🇼",
        "🇸🇦",
        "🇸🇧",
        "🇸🇨",
        "🇸🇩",
        "🇸🇪",
        "🇸🇬",
        "🇸🇭",
        "🇸🇮",
        "🇸🇯",
        "🇸🇰",
        "🇸🇱",
        "🇸🇲",
        "🇸🇳",
        "🇸🇴",
        "🇸🇷",
        "🇸🇸",
        "🇸🇹",
        "🇸🇻",
        "🇸🇽",
        "🇸🇾",
        "🇸🇿",
        "🇹🇦",
        "🇹🇨",
        "🇹🇩",
        "🇹🇫",
        "🇹🇬",
        "🇹🇭",
        "🇹🇯",
        "🇹🇰",
        "🇹🇱",
        "🇹🇲",
        "🇹🇳",
        "🇹🇴",
        "🇹🇷",
        "🇹🇹",
        "🇹🇻",
        "🇹🇼",
        "🇹🇿",
        "🇺🇦",
        "🇺🇬",
        "🇺🇲",
        "🇺🇳",
        "🇺🇸",
        "🇺🇾",
        "🇺🇿",
        "🇻🇦",
        "🇻🇨",
        "🇻🇪",
        "🇻🇬",
        "🇻🇮",
        "🇻🇳",
        "🇻🇺",
        "🇼🇫",
        "🇼🇸",
        "🇽🇰",
        "🇾🇪",
        "🇾🇹",
        "🇿🇦",
        "🇿🇲",
        "🇿🇼",
        "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
        "🏴󠁧󠁢󠁳󠁣󠁴󠁿",
        "🏴󠁧󠁢󠁷󠁬󠁳󠁿"
    );

}
