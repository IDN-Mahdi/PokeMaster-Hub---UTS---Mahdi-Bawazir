package com.example.data.repository

object PokemonNamesData {

    // Official 1025 National Pokédex Names (Index 0 = #0001 Bulbasaur .. Index 1024 = #1025 Pecharunt)
    val NAMES_1025: List<String> by lazy {
        RAW_NAMES.trim().split(",").map { it.trim() }
    }

    private const val RAW_NAMES =
        "Bulbasaur,Ivysaur,Venusaur,Charmander,Charmeleon,Charizard,Squirtle,Wartortle,Blastoise,Caterpie," +
        "Metapod,Butterfree,Weedle,Kakuna,Beedrill,Pidgey,Pidgeotto,Pidgeot,Rattata,Raticate," +
        "Spearow,Fearow,Ekans,Arbok,Pikachu,Raichu,Sandshrew,Sandslash,Nidoran♀,Nidorina," +
        "Nidoqueen,Nidoran♂,Nidorino,Nidoking,Clefairy,Clefable,Vulpix,Ninetales,Jigglypuff,Wigglytuff," +
        "Zubat,Golbat,Oddish,Gloom,Vileplume,Paras,Parasect,Venonat,Venomoth,Diglett," +
        "Dugtrio,Meowth,Persian,Psyduck,Golduck,Mankey,Primeape,Growlithe,Arcanine,Poliwag," +
        "Poliwhirl,Poliwrath,Abra,Kadabra,Alakazam,Machop,Machoke,Machamp,Bellsprout,Weepinbell," +
        "Victreebel,Tentacool,Tentacruel,Geodude,Graveler,Golem,Ponyta,Rapidash,Slowpoke,Slowbro," +
        "Magnemite,Magneton,Farfetch'd,Doduo,Dodrio,Seel,Dewgong,Grimer,Muk,Shellder," +
        "Cloyster,Gastly,Haunter,Gengar,Onix,Drowzee,Hypno,Krabby,Kingler,Voltorb," +
        "Electrode,Exeggcute,Exeggutor,Cubone,Marowak,Hitmonlee,Hitmonchan,Lickitung,Koffing,Weezing," +
        "Rhyhorn,Rhydon,Chansey,Tangela,Kangaskhan,Horsea,Seadra,Goldeen,Seaking,Staryu," +
        "Starmie,Mr. Mime,Scyther,Jynx,Electabuzz,Magmar,Pinsir,Tauros,Magikarp,Gyarados," +
        "Lapras,Ditto,Eevee,Vaporeon,Jolteon,Flareon,Porygon,Omanyte,Omastar,Kabuto," +
        "Kabutops,Aerodactyl,Snorlax,Articuno,Zapdos,Moltres,Dratini,Dragonair,Dragonite,Mewtwo,Mew," +
        "Chikorita,Bayleef,Meganium,Cyndaquil,Quilava,Typhlosion,Totodile,Croconaw,Feraligatr,Sentret," +
        "Furret,Hoothoot,Noctowl,Ledyba,Ledian,Spinarak,Ariados,Crobat,Chinchou,Lanturn," +
        "Pichu,Cleffa,Igglybuff,Togepi,Togetic,Natu,Xatu,Mareep,Flaaffy,Ampharos," +
        "Bellossom,Marill,Azumarill,Sudowoodo,Politoed,Hoppip,Skiploom,Jumpluff,Aipom,Sunkern," +
        "Sunflora,Yanma,Wooper,Quagsire,Espeon,Umbreon,Murkrow,Slowking,Misdreavus,Unown," +
        "Wobbuffet,Girafarig,Pineco,Forretress,Dunsparce,Gligar,Steelix,Snubbull,Granbull,Qwilfish," +
        "Scizor,Shuckle,Heracross,Sneasel,Teddiursa,Ursaring,Slugma,Magcargo,Swinub,Piloswine," +
        "Corsola,Remoraid,Octillery,Delibird,Mantine,Skarmory,Houndour,Houndoom,Kingdra,Phanpy," +
        "Donphan,Porygon2,Stantler,Smeargle,Tyrogue,Hitmontop,Smoochum,Elekid,Magby,Miltank," +
        "Blissey,Raikou,Entei,Suicune,Larvitar,Pupitar,Tyranitar,Lugia,Ho-Oh,Celebi," +
        "Treecko,Grovyle,Sceptile,Torchic,Combusken,Blaziken,Mudkip,Marshtomp,Swampert,Poochyena," +
        "Mightyena,Zigzagoon,Linoone,Wurmple,Silcoon,Beautifly,Cascoon,Dustox,Lotad,Lombre," +
        "Ludicolo,Seedot,Nuzleaf,Shiftry,Taillow,Swellow,Wingull,Pelipper,Ralts,Kirlia," +
        "Gardevoir,Surskit,Masquerain,Shroomish,Breloom,Slakoth,Vigoroth,Slaking,Nincada,Ninjask," +
        "Shedinja,Whismur,Loudred,Exploud,Makuhita,Hariyama,Azurill,Nosepass,Skitty,Delcatty," +
        "Sableye,Mawile,Aron,Lairon,Aggron,Meditite,Medicham,Electrike,Manectric,Plusle," +
        "Minun,Volbeat,Illumise,Roselia,Gulpin,Swalot,Carvanha,Sharpedo,Wailmer,Wailord," +
        "Numel,Camerupt,Torkoal,Spoink,Grumpig,Spinda,Trapinch,Vibrava,Flygon,Cacnea," +
        "Cacturne,Swablu,Altaria,Zangoose,Seviper,Lunatone,Solrock,Barboach,Whiscash,Corphish," +
        "Crawdaunt,Baltoy,Claydol,Lileep,Cradily,Anorith,Armaldo,Feebas,Milotic,Castform," +
        "Kecleon,Shuppet,Banette,Duskull,Dusclops,Tropius,Chimecho,Absol,Wynaut,Snorunt," +
        "Glalie,Spheal,Sealeo,Walrein,Clamperl,Huntail,Gorebyss,Relicanth,Luvdisc,Bagon," +
        "Shelgon,Salamence,Beldum,Metang,Metagross,Regirock,Regice,Registeel,Latias,Latios," +
        "Kyogre,Groudon,Rayquaza,Jirachi,Deoxys,Turtwig,Grotle,Torterra,Chimchar,Monferno," +
        "Infernape,Piplup,Prinplup,Empoleon,Starly,Staravia,Staraptor,Bidoof,Bibarel,Kricketot," +
        "Kricketune,Shinx,Luxio,Luxray,Budew,Roserade,Cranidos,Rampardos,Shieldon,Bastiodon," +
        "Burmy,Wormadam,Mothim,Combee,Vespiquen,Pachirisu,Buizel,Floatzel,Cherubi,Cherrim," +
        "Shellos,Gastrodon,Ambipom,Drifloon,Drifblim,Buneary,Lopunny,Mismagius,Honchkrow,Glameow," +
        "Purugly,Chingling,Stunky,Skuntank,Bronzor,Bronzong,Bonsly,Mime Jr.,Happiny,Chatot," +
        "Spiritomb,Gible,Gabite,Garchomp,Munchlax,Riolu,Lucario,Hippopotas,Hippowdon,Skorupi," +
        "Drapion,Croagunk,Toxicroak,Carnivine,Finneon,Lumineon,Mantyke,Snover,Abomasnow,Weavile," +
        "Magnezone,Lickilicky,Rhyperior,Tangrowth,Electivire,Magmortar,Togekiss,Yanmega,Leafeon,Glaceon," +
        "Gliscor,Mamoswine,Porygon-Z,Gallade,Probopass,Dusknoir,Froslass,Rotom,Uxie,Mesprit," +
        "Azelf,Dialga,Palkia,Heatran,Regigigas,Giratina,Cresselia,Phione,Manaphy,Darkrai," +
        "Shaymin,Arceus,Victini,Snivy,Servine,Serperior,Tepig,Pignite,Emboar,Oshawott," +
        "Dewott,Samurott,Patrat,Watchog,Lillipup,Herdier,Stoutland,Purrloin,Liepard,Pansage," +
        "Simisage,Pansear,Simisear,Panpour,Simipour,Munna,Musharna,Pidove,Tranquill,Unfezant," +
        "Blitzle,Zebstrika,Roggenrola,Boldore,Gigalith,Woobat,Swoobat,Drilbur,Excadrill,Audino," +
        "Timburr,Gurdurr,Conkeldurr,Tympole,Palpitoad,Seismitoad,Throh,Sawk,Sewaddle,Swadloon," +
        "Leavanny,Venipede,Whirlipede,Scolipede,Cottonee,Whimsicott,Petilil,Lilligant,Basculin,Sandile," +
        "Krokorok,Krookodile,Darumaka,Darmanitan,Maractus,Dwebble,Crustle,Scraggy,Scrafty,Sigilyph," +
        "Yamask,Cofagrigus,Tirtouga,Carracosta,Archen,Archeops,Trubbish,Garbodor,Zorua,Zoroark," +
        "Minccino,Cinccino,Gothita,Gothorita,Gothitelle,Solosis,Duosion,Reuniclus,Ducklett,Swanna," +
        "Vanillite,Vanillish,Vanilluxe,Deerling,Sawsbuck,Emolga,Karrablast,Escavalier,Foongus,Amoonguss," +
        "Frillish,Jellicent,Alomomola,Joltik,Galvantula,Ferroseed,Ferrothorn,Klink,Klang,Klinklang," +
        "Tynamo,Eelektrik,Eelektross,Elgyem,Beheeyem,Litwick,Lampent,Chandelure,Axew,Fraxure," +
        "Haxorus,Cubchoo,Beartic,Cryogonal,Shelmet,Accelgor,Stunfisk,Mienfoo,Mienshao,Druddigon," +
        "Golett,Golurk,Pawniard,Bisharp,Bouffalant,Rufflet,Braviary,Vullaby,Mandibuzz,Heatmor," +
        "Durant,Deino,Zweilous,Hydreigon,Larvesta,Volcarona,Cobalion,Terrakion,Virizion,Tornadus," +
        "Thundurus,Reshiram,Zekrom,Landorus,Kyurem,Keldeo,Meloetta,Genesect,Chespin,Quilladin," +
        "Chesnaught,Fennekin,Braixen,Delphox,Froakie,Frogadier,Greninja,Bunnelby,Diggersby,Fletchling," +
        "Fletchinder,Talonflame,Scatterbug,Spewpa,Vivillon,Litleo,Pyroar,Flabébé,Floette,Florges," +
        "Skiddo,Gogoat,Pancham,Pangoro,Furfrou,Espurr,Meowstic,Honedge,Doublade,Aegislash," +
        "Spritzee,Aromatisse,Swirlix,Slurpuff,Inkay,Malamar,Binacle,Barbaracle,Skrelp,Dragalge," +
        "Clauncher,Clawitzer,Helioptile,Heliolisk,Tyrunt,Tyrantrum,Amaura,Aurorus,Sylveon,Hawlucha," +
        "Dedenne,Carbink,Goomy,Sliggoo,Goodra,Klefki,Phantump,Trevenant,Pumpkaboo,Gourgeist," +
        "Bergmite,Avalugg,Noibat,Noivern,Xerneas,Yveltal,Zygarde,Diancie,Hoopa,Volcanion," +
        "Rowlet,Dartrix,Decidueye,Litten,Torracat,Incineroar,Popplio,Brionne,Primarina,Pikipek," +
        "Trumbeak,Toucannon,Yungoos,Gumshoos,Grubbin,Charjabug,Vikavolt,Crabrawler,Crabominable,Oricorio," +
        "Cutiefly,Ribombee,Rockruff,Lycanroc,Wishiwashi,Mareanie,Toxapex,Mudbray,Mudsdale,Dewpider," +
        "Araquanid,Fomantis,Lurantis,Morelull,Shiinotic,Salandit,Salazzle,Stufful,Bewear,Bounsweet," +
        "Steenee,Tsareena,Comfey,Oranguru,Passimian,Wimpod,Golisopod,Sandygast,Palossand,Pyukumuku," +
        "Type: Null,Silvally,Minior,Komala,Turtonator,Togedemaru,Mimikyu,Bruxish,Drampa,Dhelmise," +
        "Jangmo-o,Hakamo-o,Kommo-o,Tapu Koko,Tapu Lele,Tapu Bulu,Tapu Fini,Cosmog,Cosmoem,Solgaleo," +
        "Lunala,Nihilego,Buzzwole,Pheromosa,Xurkitree,Celesteela,Kartana,Guzzlord,Necrozma,Magearna," +
        "Marshadow,Poipole,Naganadel,Stakataka,Blacephalon,Zeraora,Meltan,Melmetal,Grookey,Thwackey," +
        "Rillaboom,Scorbunny,Raboot,Cinderace,Sobble,Drizzile,Inteleon,Skwovet,Greedent,Rookidee," +
        "Corvisquire,Corviknight,Blipbug,Dottler,Orbeetle,Nickit,Thievul,Gossifleur,Eldegoss,Wooloo," +
        "Dubwool,Chewtle,Drednaw,Yamper,Boltund,Rolycoly,Carkol,Coalossal,Applin,Flapple," +
        "Appletun,Silicobra,Sandaconda,Cramorant,Arrokuda,Barraskewda,Toxel,Toxtricity,Sizzlipede,Centiskorch," +
        "Clobbopus,Grapploct,Sinistea,Polteageist,Hatenna,Hattrem,Hatterene,Impidimp,Morgrem,Grimmsnarl," +
        "Obstagoon,Perrserker,Cursola,Sirfetch'd,Mr. Rime,Runerigus,Milcery,Alcremie,Falinks,Pincurchin," +
        "Snom,Frosmoth,Stonjourner,Eiscue,Indeedee,Morpeko,Cufant,Copperajah,Dracozolt,Arctozolt," +
        "Dracovish,Arctovish,Duraludon,Dreepy,Drakloak,Dragapult,Zacian,Zamazenta,Eternatus,Kubfu," +
        "Urshifu,Zarude,Regieleki,Regidrago,Glastrier,Spectrier,Calyrex,Wyrdeer,Kleavor,Ursaluna," +
        "Basculegion,Sneasler,Overqwil,Enamorus,Sprigatito,Floragato,Meowscarada,Fuecoco,Crocalor,Skeledirge," +
        "Quaxly,Quaxwell,Quaquaval,Lechonk,Oinkologne,Tarountula,Spidops,Nymble,Lokix,Pawmi," +
        "Pawmo,Pawmot,Tandemaus,Maushold,Fidough,Dachsbun,Smoliv,Dolliv,Arboliva,Squawkabilly," +
        "Nacli,Naclstack,Garganacl,Charcadet,Armarouge,Ceruledge,Tadbulb,Bellibolt,Wattrel,Kilowattrel," +
        "Maschiff,Mabosstiff,Shroodle,Grafaiai,Bramblin,Brambleghast,Toedscool,Toedscruel,Klawf,Capsakid," +
        "Scovillain,Rellor,Rabsca,Flittle,Espathra,Tinkatink,Tinkatuff,Tinkaton,Wiglett,Wugtrio," +
        "Bombirdier,Finizen,Palafin,Varoom,Revavroom,Cyclizar,Orthworm,Glimmet,Glimmora,Greavard," +
        "Houndstone,Flamigo,Cetoddle,Cetitan,Veluza,Dondozo,Tatsugiri,Annihilape,Clodsire,Farigiraf," +
        "Dudunsparce,Kingambit,Great Tusk,Scream Tail,Brute Bonnet,Flutter Mane,Slither Wing,Sandy Shocks,Iron Treads,Iron Bundle," +
        "Iron Hands,Iron Jugulis,Iron Moth,Iron Thorns,Frigibax,Arctibax,Baxcalibur,Gimmighoul,Gholdengo,Wo-Chien," +
        "Chien-Pao,Ting-Lu,Chi-Yu,Roaring Moon,Iron Valiant,Koraidon,Miraidon,Walking Wake,Iron Leaves,Dipplin," +
        "Poltchageist,Sinistcha,Okidogi,Munkidori,Fezandipiti,Ogerpon,Archaludon,Hydrapple,Gouging Fire,Raging Bolt," +
        "Iron Boulder,Iron Crown,Terapagos,Pecharunt"

    // Exact 256 HGSS Johto Pokédex order (National Dex IDs in #001..#256 order)
    val HGSS_JOHTO_256_IDS: List<Int>
        get() = PokemonCanonicalData.OFFICIAL_JOHTO_256

    // Exact 242 Hisui Pokédex order (Pokémon Legends: Arceus #001 Rowlet .. #242 Arceus)
    val HISUI_242_IDS: List<Int>
        get() = PokemonCanonicalData.OFFICIAL_HISUI_242

    // Exact 457 Kalos Pokédex order (Central + Coastal + Mountain #001 Chespin .. #457 Volcanion)
    val KALOS_457_IDS: List<Int>
        get() = PokemonCanonicalData.OFFICIAL_KALOS_457

    // Canonical Evolution Families across National Dex 1..1025
    val CANONICAL_FAMILIES: List<List<Int>>
        get() = PokemonCanonicalData.OFFICIAL_FAMILIES

    // 129 Canonical Johto Evolution Families (in Johto Dex order)
    val JOHTO_FAMILIES: List<List<Int>> = listOf(
        listOf(152, 153, 154), // Chikorita line
        listOf(155, 156, 157), // Cyndaquil line
        listOf(158, 159, 160), // Totodile line
        listOf(16, 17, 18),    // Pidgey line
        listOf(21, 22),        // Spearow line
        listOf(163, 164),      // Hoothoot line
        listOf(19, 20),        // Rattata line
        listOf(161, 162),      // Sentret line
        listOf(172, 25, 26),   // Pichu / Pikachu line
        listOf(10, 11, 12),    // Caterpie line
        listOf(13, 14, 15),    // Weedle line
        listOf(165, 166),      // Ledyba line
        listOf(167, 168),      // Spinarak line
        listOf(74, 75, 76),    // Geodude line
        listOf(41, 42, 169),   // Zubat line
        listOf(173, 35, 36),   // Cleffa / Clefairy line
        listOf(174, 39, 40),   // Igglybuff / Jigglypuff line
        listOf(175, 176),      // Togepi / Togetic line
        listOf(27, 28),        // Sandshrew line
        listOf(23, 24),        // Ekans / Arbok line
        listOf(206),           // Dunsparce
        listOf(179, 180, 181), // Mareep line
        listOf(194, 195),      // Wooper line
        listOf(92, 93, 94),    // Gastly line
        listOf(201),           // Unown
        listOf(95, 208),       // Onix / Steelix line
        listOf(69, 70, 71),    // Bellsprout line
        listOf(187, 188, 189), // Hoppip line
        listOf(46, 47),        // Paras line
        listOf(60, 61, 62, 186), // Poliwag / Poliwrath / Politoed
        listOf(129, 130),      // Magikarp / Gyarados
        listOf(118, 119),      // Goldeen / Seaking
        listOf(79, 80, 199),   // Slowpoke / Slowbro / Slowking
        listOf(43, 44, 45, 182), // Oddish / Vileplume / Bellossom
        listOf(96, 97),        // Drowzee / Hypno
        listOf(63, 64, 65),    // Abra / Kadabra / Alakazam
        listOf(132),           // Ditto
        listOf(204, 205),      // Pineco / Forretress
        listOf(29, 30, 31),    // Nidoran F line
        listOf(32, 33, 34),    // Nidoran M line
        listOf(193, 469),      // Yanma / Yanmega
        listOf(191, 192),      // Sunkern / Sunflora
        listOf(190, 424),      // Aipom / Ambipom
        listOf(209, 210),      // Snubbull / Granbull
        listOf(37, 38),        // Vulpix / Ninetales
        listOf(58, 59),        // Growlithe / Arcanine
        listOf(234),           // Stantler
        listOf(183, 184),      // Marill / Azumarill
        listOf(185),           // Sudowoodo
        listOf(102, 103),      // Exeggcute / Exeggutor
        listOf(48, 49),        // Venonat / Venomoth
        listOf(123, 212),      // Scyther / Scizor
        listOf(127),           // Pinsir
        listOf(214),           // Heracross
        listOf(109, 110),      // Koffing / Weezing
        listOf(88, 89),        // Grimer / Muk
        listOf(81, 82),        // Magnemite / Magneton
        listOf(100, 101),      // Voltorb / Electrode
        listOf(56, 57),        // Mankey / Primeape
        listOf(52, 53),        // Meowth / Persian
        listOf(54, 55),        // Psyduck / Golduck
        listOf(66, 67, 68),    // Machop / Machoke / Machamp
        listOf(236, 106, 107, 237), // Tyrogue / Hitmonlee / Hitmonchan / Hitmontop
        listOf(203),           // Girafarig
        listOf(128),           // Tauros
        listOf(241),           // Miltank
        listOf(240, 126),      // Magby / Magmar
        listOf(238, 124),      // Smoochum / Jynx
        listOf(239, 125),      // Elekid / Electabuzz
        listOf(122),           // Mr. Mime
        listOf(235),           // Smeargle
        listOf(83),            // Farfetch'd
        listOf(177, 178),      // Natu / Xatu
        listOf(211),           // Qwilfish
        listOf(72, 73),        // Tentacool / Tentacruel
        listOf(98, 99),        // Krabby / Kingler
        listOf(213),           // Shuckle
        listOf(120, 121),      // Staryu / Starmie
        listOf(90, 91),        // Shellder / Cloyster
        listOf(222),           // Corsola
        listOf(223, 224),      // Remoraid / Octillery
        listOf(170, 171),      // Chinchou / Lanturn
        listOf(86, 87),        // Seel / Dewgong
        listOf(108, 463),      // Lickitung / Lickilicky
        listOf(114, 465),      // Tangela / Tangrowth
        listOf(133, 134, 135, 136, 196, 197), // Eevee & Johto Eeveelutions
        listOf(116, 117, 230), // Horsea / Seadra / Kingdra
        listOf(207),           // Gligar
        listOf(225),           // Delibird
        listOf(220, 221, 473), // Swinub / Piloswine / Mamoswine
        listOf(216, 217),      // Teddiursa / Ursaring
        listOf(231, 232),      // Phanpy / Donphan
        listOf(226),           // Mantine
        listOf(227),           // Skarmory
        listOf(84, 85),        // Doduo / Dodrio
        listOf(77, 78),        // Ponyta / Rapidash
        listOf(104, 105),      // Cubone / Marowak
        listOf(115),           // Kangaskhan
        listOf(111, 112),      // Rhyhorn / Rhydon
        listOf(198),           // Murkrow
        listOf(228, 229),      // Houndour / Houndoom
        listOf(218, 219),      // Slugma / Magcargo
        listOf(215),           // Sneasel
        listOf(200),           // Misdreavus
        listOf(137, 233),      // Porygon / Porygon2
        listOf(113, 242),      // Chansey / Blissey
        listOf(131),           // Lapras
        listOf(138, 139),      // Omanyte / Omastar
        listOf(140, 141),      // Kabuto / Kabutops
        listOf(142),           // Aerodactyl
        listOf(143),           // Snorlax
        listOf(50, 51),        // Diglett / Dugtrio
        listOf(202),           // Wobbuffet
        listOf(147, 148, 149), // Dratini / Dragonair / Dragonite
        listOf(246, 247, 248), // Larvitar / Pupitar / Tyranitar
        listOf(144),           // Articuno
        listOf(145),           // Zapdos
        listOf(146),           // Moltres
        listOf(243),           // Raikou
        listOf(244),           // Entei
        listOf(245),           // Suicune
        listOf(249),           // Lugia
        listOf(250),           // Ho-Oh
        listOf(150),           // Mewtwo
        listOf(151),           // Mew
        listOf(251),           // Celebi
        listOf(1, 2, 3),       // Bulbasaur line (Kanto post-game HGSS)
        listOf(4, 5, 6),       // Charmander line (Kanto post-game HGSS)
        listOf(7, 8, 9)        // Squirtle line (Kanto post-game HGSS)
    )
}
