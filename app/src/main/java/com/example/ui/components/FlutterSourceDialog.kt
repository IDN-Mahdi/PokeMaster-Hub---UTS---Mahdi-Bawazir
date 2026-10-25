package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceHeader
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonEmerald

enum class FlutterSourceFile(val filename: String, val description: String) {
    PUBSPEC("pubspec.yaml", "Dependencies configuration (http, sqflite, shared_preferences, path_provider, audioplayers)"),
    MODEL("lib/models/pokemon_model.dart", "Model Class with factory PokemonModel.fromJson & toJson (Syarat Wajib UTS)"),
    POKEAPI("lib/services/pokeapi_service.dart", "HTTP GET + PokemonModel.fromJson to pokeapi.co/api/v2 (UTS Pemrograman Mobile)"),
    PERSISTENCE("lib/services/persistence_service.dart", "3 Data Persistence: SharedPreferences + SQLite + File JSON"),
    MAIN("lib/main.dart", "Multi-Route UI (BottomNavigationBar + Navigator.push), FutureBuilder 3-State, & mounted guard")
}

@Composable
fun FlutterSourceDialog(onClose: () -> Unit) {
    var selectedFile by remember { mutableStateOf(FlutterSourceFile.PUBSPEC) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val codeContent = remember(selectedFile) {
        when (selectedFile) {
            FlutterSourceFile.PUBSPEC -> getPubspecCode()
            FlutterSourceFile.MODEL -> getPokemonModelCode()
            FlutterSourceFile.POKEAPI -> getPokeApiServiceCode()
            FlutterSourceFile.PERSISTENCE -> getPersistenceServiceCode()
            FlutterSourceFile.MAIN -> getMainDartCode()
        }
    }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                .testTag("flutter_source_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DataObject, contentDescription = null, tint = NeonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Flutter & Dart Source Code Exporter",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Syarat Wajib UTS & UAS Mobile / Multiplatform",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // File Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlutterSourceFile.entries.forEach { f ->
                        val isSel = selectedFile == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonBlue.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                .border(1.dp, if (isSel) NeonBlue else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { selectedFile = f }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = f.filename,
                                color = if (isSel) NeonBlue else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = selectedFile.description,
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Code Viewer Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF060913))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = codeContent,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Copy to Clipboard Button
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(codeContent))
                        Toast.makeText(context, "${selectedFile.filename} copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy ${selectedFile.filename} to Clipboard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getPubspecCode(): String = """
name: pokemaster_hub
description: "PokéMaster Hub: National 1025 & HGSS Tactical Suite"
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.2.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  # REST API HTTP Client for pokeapi.co v2 (Syarat UTS Pemrograman Mobile)
  http: ^1.2.0
  # Data Persistence Teknik 1: Preferences
  shared_preferences: ^2.2.2
  # Data Persistence Teknik 2: SQLite Relational Database
  sqflite: ^2.3.2
  path: ^1.9.0
  # Data Persistence Teknik 3: Read/Write Local JSON Files
  path_provider: ^2.1.2
  # Audio cry streaming from PokeAPI CDN
  audioplayers: ^6.0.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
""".trimIndent()

private fun getPokemonModelCode(): String {
    val s = "$"
    return """
/// Model Class Resmi untuk Konversi JSON PokéAPI v2 & Penyimpanan Lokal
class PokemonModel {
  final int id;
  final String name;
  final List<String> types;
  final double heightMeters;
  final double weightKg;
  final int baseExperience;
  final Map<String, int> baseStats;
  final List<String> abilities;
  final List<String> moves;
  final String spriteUrl;
  final String cryUrl;
  final String flavorText;

  const PokemonModel({
    required this.id,
    required this.name,
    required this.types,
    required this.heightMeters,
    required this.weightKg,
    required this.baseExperience,
    required this.baseStats,
    required this.abilities,
    required this.moves,
    required this.spriteUrl,
    required this.cryUrl,
    this.flavorText = '',
  });

  int get bst => baseStats.values.fold(0, (sum, val) => sum + val);

  /// Factory Constructor untuk Deserialisasi JSON dari PokéAPI v2
  factory PokemonModel.fromJson(Map<String, dynamic> json, {String flavorText = ''}) {
    final int id = json['id'] as int? ?? 1;
    final String rawName = (json['name'] ?? 'pokemon').toString();
    final String formattedName = rawName.isNotEmpty
        ? '${s}{rawName[0].toUpperCase()}${s}{rawName.substring(1)}'
        : 'Pokemon';

    final List<String> parsedTypes = (json['types'] as List<dynamic>? ?? [])
        .map((t) {
          final typeName = t['type']['name'].toString();
          return '${s}{typeName[0].toUpperCase()}${s}{typeName.substring(1)}';
        })
        .toList();

    final Map<String, int> parsedStats = {};
    for (final sItem in (json['stats'] as List<dynamic>? ?? [])) {
      final statName = sItem['stat']['name'].toString();
      final baseStat = sItem['base_stat'] as int? ?? 50;
      parsedStats[statName] = baseStat;
    }

    final List<String> parsedAbilities = (json['abilities'] as List<dynamic>? ?? [])
        .map((a) => a['ability']['name'].toString())
        .toList();

    final List<String> parsedMoves = (json['moves'] as List<dynamic>? ?? [])
        .take(15)
        .map((m) => m['move']['name'].toString())
        .toList();

    return PokemonModel(
      id: id,
      name: formattedName,
      types: parsedTypes.isEmpty ? ['Normal'] : parsedTypes,
      heightMeters: ((json['height'] as int? ?? 10) / 10.0),
      weightKg: ((json['weight'] as int? ?? 100) / 10.0),
      baseExperience: json['base_experience'] as int? ?? 64,
      baseStats: parsedStats,
      abilities: parsedAbilities,
      moves: parsedMoves,
      spriteUrl: 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${s}id.png',
      cryUrl: 'https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest/${s}id.ogg',
      flavorText: flavorText,
    );
  }

  /// Konversi Objek Dart kembali ke Map JSON untuk Penyimpanan Lokal / Cache
  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'types': types,
        'heightMeters': heightMeters,
        'weightKg': weightKg,
        'baseExperience': baseExperience,
        'baseStats': baseStats,
        'abilities': abilities,
        'moves': moves,
        'spriteUrl': spriteUrl,
        'cryUrl': cryUrl,
        'flavorText': flavorText,
      };
}
""".trimIndent()
}

private fun getPokeApiServiceCode(): String {
    val s = "$"
    return """
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/pokemon_model.dart';

/// Layanan Live REST API PokéAPI v2 (Syarat Wajib UTS Pemrograman Mobile)
class PokeApiService {
  static const String baseUrl = 'https://pokeapi.co/api/v2';

  /// Melakukan HTTP GET ke endpoint pokemon/{id} & pokemon-species/{id}
  /// lalu mengonversi JSON menjadi objek PokemonModel melalui factory fromJson
  Future<PokemonModel> fetchPokemonDetail(int id) async {
    try {
      final pokemonUri = Uri.parse('${s}baseUrl/pokemon/${s}id');
      final response = await http.get(pokemonUri).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = jsonDecode(response.body);
        final String flavor = await fetchFlavorText(id);
        return PokemonModel.fromJson(data, flavorText: flavor);
      } else {
        throw Exception('Gagal memuat Pokémon #${s}id: HTTP ${s}{response.statusCode}');
      }
    } catch (e) {
      throw Exception('Kesalahan jaringan saat menghubungi PokéAPI: ${s}e');
    }
  }

  /// Melakukan HTTP GET ke https://pokeapi.co/api/v2/pokemon-species/{id}
  Future<String> fetchFlavorText(int id) async {
    try {
      final uri = Uri.parse('${s}baseUrl/pokemon-species/${s}id');
      final response = await http.get(uri).timeout(const Duration(seconds: 8));

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = jsonDecode(response.body);
        final entries = data['flavor_text_entries'] as List<dynamic>? ?? [];
        for (final entry in entries) {
          if (entry['language']['name'] == 'en') {
            return entry['flavor_text']
                .toString()
                .replaceAll('\n', ' ')
                .replaceAll('\f', ' ');
          }
        }
      }
    } catch (_) {
      // Fallback jika endpoint species timeout
    }
    return 'Deskripsi resmi Pokédex sedang dalam mode offline.';
  }
}
""".trimIndent()
}

private fun getPersistenceServiceCode(): String {
    val s = "$"
    return """
import 'dart:io';
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';

/// 3 Teknik Data Persistence Sekaligus (Syarat Wajib UTS & UAS Pemrograman Mobile)
class PersistenceService {
  static Database? _database;

  // -------------------------------------------------------------
  // TEKNIK 1: SharedPreferences (Preferensi Pengguna & Streak)
  // -------------------------------------------------------------
  Future<void> saveMechanicMode(String mode) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('mechanic_mode', mode); // 'gen4' vs 'modern'
  }

  Future<String> getMechanicMode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('mechanic_mode') ?? 'modern';
  }

  Future<void> saveShinyMode(bool isShiny) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('is_shiny_mode', isShiny);
  }

  Future<bool> getShinyMode() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool('is_shiny_mode') ?? false;
  }

  // -------------------------------------------------------------
  // TEKNIK 2: SQLite Relational Database (sqflite)
  // -------------------------------------------------------------
  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'pokemaster_hub.db');

    return await openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE pokemon_ratings (
            pokemon_id INTEGER PRIMARY KEY,
            status TEXT NOT NULL,
            tags TEXT NOT NULL,
            custom_tier TEXT,
            updated_at INTEGER NOT NULL
          )
        ''');

        await db.execute('''
          CREATE TABLE pokedex_tracker (
            pokemon_id INTEGER PRIMARY KEY,
            status TEXT NOT NULL,
            updated_at INTEGER NOT NULL
          )
        ''');
      },
    );
  }

  Future<void> upsertRating(int id, String status, List<String> tags, String? customTier) async {
    final db = await database;
    await db.insert(
      'pokemon_ratings',
      {
        'pokemon_id': id,
        'status': status,
        'tags': tags.join(','),
        'custom_tier': customTier,
        'updated_at': DateTime.now().millisecondsSinceEpoch,
      },
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<Map<int, String>> getAllRatings() async {
    final db = await database;
    final rows = await db.query('pokemon_ratings');
    return {
      for (final r in rows) (r['pokemon_id'] as int): (r['status'] as String),
    };
  }

  // -------------------------------------------------------------
  // TEKNIK 3: Read and Write Files (path_provider & dart:io)
  // -------------------------------------------------------------
  Future<File> get _autosaveFile async {
    final directory = await getApplicationDocumentsDirectory();
    return File('${s}{directory.path}/pokemaster_autosave.json');
  }

  Future<void> exportAutosaveJson(Map<String, dynamic> state) async {
    final file = await _autosaveFile;
    await file.writeAsString(jsonEncode(state));
  }

  Future<Map<String, dynamic>?> importAutosaveJson() async {
    try {
      final file = await _autosaveFile;
      if (await file.exists()) {
        final content = await file.readAsString();
        return jsonDecode(content) as Map<String, dynamic>;
      }
    } catch (e) {
      // Fallback aman apabila file belum ada atau korup
    }
    return null;
  }
}
""".trimIndent()
}

private fun getMainDartCode(): String {
    val s = "$"
    return """
import 'package:flutter/material.dart';
import 'models/pokemon_model.dart';
import 'services/pokeapi_service.dart';
import 'services/persistence_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const PokeMasterApp());
}

class PokeMasterApp extends StatelessWidget {
  const PokeMasterApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PokéMaster Hub: National 1025 & HGSS',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF020617),
        cardColor: const Color(0xFF0F172A),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF38BDF8),
          secondary: Color(0xFFF59E0B),
          surface: Color(0xFF0F172A),
        ),
      ),
      initialRoute: '/',
      routes: {
        '/': (context) => const MainShellScreen(),
        '/party-lab': (context) => const PartyLabScreen(),
      },
    );
  }
}

/// Halaman Induk dengan BottomNavigationBar (Minimal 3 Halaman/Rute)
class MainShellScreen extends StatefulWidget {
  const MainShellScreen({super.key});

  @override
  State<MainShellScreen> createState() => _MainShellScreenState();
}

class _MainShellScreenState extends State<MainShellScreen> {
  int _selectedIndex = 0;
  final PersistenceService _persistence = PersistenceService();
  String _mechanicMode = 'modern';
  Map<int, String> _ratings = {};

  @override
  void initState() {
    super.initState();
    _loadInitialPersistence();
  }

  Future<void> _loadInitialPersistence() async {
    try {
      final mode = await _persistence.getMechanicMode();
      final savedRatings = await _persistence.getAllRatings();
      if (!mounted) return;
      setState(() {
        _mechanicMode = mode;
        _ratings = savedRatings;
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Gagal memuat data lokal: ${s}e')),
      );
    }
  }

  Future<void> _toggleFavorite(int pokemonId) async {
    try {
      final nextStatus = _ratings[pokemonId] == 'LIKE' ? 'NONE' : 'LIKE';
      await _persistence.upsertRating(pokemonId, nextStatus, ['Favorite'], 'S');
      await _persistence.exportAutosaveJson({
        'mechanicMode': _mechanicMode,
        'lastUpdatedPokemon': pokemonId,
        'status': nextStatus,
      });
      if (!mounted) return;
      setState(() {
        _ratings[pokemonId] = nextStatus;
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Gagal menyimpan favorit: ${s}e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      NationalDexGridPage(
        ratings: _ratings,
        onToggleFavorite: _toggleFavorite,
      ),
      const JohtoCuratorPage(),
      const PartyLabScreen(),
    ];

    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF0F172A),
        title: Text('PokéMaster Hub (${s}{_mechanicMode.toUpperCase()})'),
        actions: [
          TextButton(
            onPressed: () async {
              final next = _mechanicMode == 'gen4' ? 'modern' : 'gen4';
              await _persistence.saveMechanicMode(next);
              if (!mounted) return;
              setState(() => _mechanicMode = next);
            },
            child: Text(_mechanicMode == 'gen4' ? 'Gen 4 (17T)' : 'Gen 6+ (18T)'),
          ),
        ],
      ),
      body: pages[_selectedIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        backgroundColor: const Color(0xFF0F172A),
        selectedItemColor: const Color(0xFF38BDF8),
        unselectedItemColor: const Color(0xFF64748B),
        onTap: (idx) => setState(() => _selectedIndex = idx),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.grid_view), label: 'Pokédex'),
          BottomNavigationBarItem(icon: Icon(Icons.auto_awesome_mosaic), label: 'Johto HGSS'),
          BottomNavigationBarItem(icon: Icon(Icons.groups), label: 'Party Lab'),
        ],
      ),
    );
  }
}

/// RUTE 1: Halaman Home / Grid Daftar 1025 Pokémon
class NationalDexGridPage extends StatelessWidget {
  final Map<int, String> ratings;
  final Future<void> Function(int) onToggleFavorite;

  const NationalDexGridPage({
    super.key,
    required this.ratings,
    required this.onToggleFavorite,
  });

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      padding: const EdgeInsets.all(12),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 0.85,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
      ),
      itemCount: 1025,
      itemBuilder: (context, index) {
        final id = index + 1;
        final isLiked = ratings[id] == 'LIKE';
        return Card(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          child: InkWell(
            borderRadius: BorderRadius.circular(14),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => PokemonDetailScreen(pokemonId: id),
                ),
              );
            },
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('#${s}{id.toString().padLeft(4, '0')}',
                    style: const TextStyle(color: Colors.grey, fontSize: 12)),
                Image.network(
                  'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${s}id.png',
                  width: 84,
                  height: 84,
                ),
                IconButton(
                  icon: Icon(
                    isLiked ? Icons.favorite : Icons.favorite_border,
                    color: isLiked ? Colors.amber : Colors.white54,
                  ),
                  onPressed: () => onToggleFavorite(id),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// RUTE 2: Halaman Kategori / Johto HGSS Curator
class JohtoCuratorPage extends StatelessWidget {
  const JohtoCuratorPage({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: const [
        ListTile(
          title: Text('129 Johto Evolution Families'),
          subtitle: Text('Seen, Caught, Unwanted, & Favorite Matrix'),
        ),
        ListTile(
          title: Text('17/18 Type Specialists Board'),
          subtitle: Text('Quick Set All & Dual-Type Slot Transfer'),
        ),
        ListTile(
          title: Text('256 Johto Pokédex Tracker'),
          subtitle: Text('Real-time Caught / Seen / Uncaught Progress'),
        ),
      ],
    );
  }
}

/// RUTE 3: Halaman Party Lab & Tactical Coverage
class PartyLabScreen extends StatelessWidget {
  const PartyLabScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text('6-Slot Party Lab: 4 Moves + Physical/Special + Secret Type'),
    );
  }
}

/// RUTE 4 (Detail Page via Navigator.push): Memakai FutureBuilder 3 Kondisi
class PokemonDetailScreen extends StatefulWidget {
  final int pokemonId;
  const PokemonDetailScreen({super.key, required this.pokemonId});

  @override
  State<PokemonDetailScreen> createState() => _PokemonDetailScreenState();
}

class _PokemonDetailScreenState extends State<PokemonDetailScreen> {
  final PokeApiService _apiService = PokeApiService();
  late Future<PokemonModel> _pokemonFuture;

  @override
  void initState() {
    super.initState();
    _pokemonFuture = _apiService.fetchPokemonDetail(widget.pokemonId);
  }

  void _refreshFromApi() {
    setState(() {
      _pokemonFuture = _apiService.fetchPokemonDetail(widget.pokemonId);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Detail Pokémon #${s}{widget.pokemonId}'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _refreshFromApi,
          ),
        ],
      ),
      body: FutureBuilder<PokemonModel>(
        future: _pokemonFuture,
        builder: (context, snapshot) {
          // 1. Kondisi Loading
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(color: Color(0xFF38BDF8)),
            );
          }
          // 2. Kondisi Error
          if (snapshot.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Gagal memuat data API:\n${s}{snapshot.error}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(color: Colors.redAccent),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton(
                      onPressed: _refreshFromApi,
                      child: const Text('Coba Lagi'),
                    ),
                  ],
                ),
              ),
            );
          }
          // 3. Kondisi Success
          final pokemon = snapshot.data!;
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Center(
                child: Image.network(pokemon.spriteUrl, height: 160),
              ),
              const SizedBox(height: 12),
              Text(
                '${s}{pokemon.name} (BST ${s}{pokemon.bst})',
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 6),
              Text(
                'Tipe: ${s}{pokemon.types.join(" / ")} | Tinggi: ${s}{pokemon.heightMeters} m | Berat: ${s}{pokemon.weightKg} kg',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text(pokemon.flavorText),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
""".trimIndent()
}


