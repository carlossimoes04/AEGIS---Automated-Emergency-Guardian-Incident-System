package cmov.grupo16.aegis.feature_scanner.data.remote.dto

/*
Data classes for the Marvel Fandom API.

Green Goblin example:
{"batchcomplete":"",
    "query":{"normalized":
        [{"from":"Green_Goblin","to":"Green Goblin"}],
    "pages":{"343614":{"pageid":343614,"ns":0,"title":"Green Goblin",
        "thumbnail":{
            "source":
                "https://static.wikia.nocookie.net/marvelcinematicuniverse/images/3/39/Green_Goblin_Infobox.png/revision/latest/scale-to-width-down/472?cb=20231202042217",
            "width":472,"height":600},
            "pageimage":"Green_Goblin_Infobox.png"}
        }
    }
}
*/

/**
 * Root response wrapper returned by the MCU Fandom MediaWiki API.
 */
data class FandomResponse(
    val query: FandomQuery? = null
)

/**
 * Holds the query results, mapping dynamic page IDs (e.g., "12345" or "-1" if not found)
 * to their corresponding wiki page details.
 */
data class FandomQuery(
    val pages: Map<String, FandomPage>? = null
)

/**
 * Represents a single MCU Fandom wiki page for a matched villain.
 */
data class FandomPage(
    val pageid: Int? = null,
    val title: String? = null,
    val thumbnail: FandomThumbnail? = null
)

/**
 * Metadata for the official page thumbnail image, including its direct URL and dimensions.
 */
data class FandomThumbnail(
    val source: String? = null,
    val width: Int? = null,
    val height: Int? = null
)