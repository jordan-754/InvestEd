package com.example.InvestEd.ui.register

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentRegisterBinding
import com.example.InvestEd.viewmodel.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    // ✅ All Philippine locations as "City, Province" strings
    private val philippineLocations: List<String> by lazy {
        mapOf(
            "Metro Manila (NCR)" to listOf(
                "Caloocan", "Las Piñas", "Makati", "Malabon", "Mandaluyong",
                "Manila", "Marikina", "Muntinlupa", "Navotas", "Parañaque",
                "Pasay", "Pasig", "Pateros", "Quezon City", "San Juan",
                "Taguig", "Valenzuela"
            ),
            "Abra" to listOf("Bangued", "Boliney", "Bucay", "Bucloc", "Daguioman",
                "Danglas", "Dolores", "La Paz", "Lacub", "Lagangilang",
                "Lagayan", "Langiden", "Luba", "Malibcong", "Manabo",
                "Peñarrubia", "Pidigan", "Pilar", "Sallapadan", "San Isidro",
                "San Juan", "San Quintin", "Tayum", "Tineg", "Tubo", "Villaviciosa"),
            "Agusan del Norte" to listOf("Buenavista", "Butuan City", "Cabadbaran City",
                "Carmen", "Jabonga", "Kitcharao", "Las Nieves", "Libertad",
                "Magallanes", "Nasipit", "Santiago", "Tubay"),
            "Agusan del Sur" to listOf("Bayugan City", "Bunawan", "Esperanza", "La Paz",
                "Loreto", "Prosperidad", "Rosario", "San Francisco",
                "San Luis", "Santa Josefa", "Sibagat", "Talacogon",
                "Trento", "Veruela"),
            "Aklan" to listOf("Altavas", "Balete", "Banga", "Batan", "Buruanga",
                "Ibajay", "Kalibo", "Lezo", "Libacao", "Madalag",
                "Makato", "Malay", "Malinao", "Nabas", "New Washington",
                "Numancia", "Tangalan"),
            "Albay" to listOf("Bacacay", "Camalig", "Daraga", "Guinobatan", "Jovellar",
                "Legazpi City", "Libon", "Ligao City", "Malilipot", "Malinao",
                "Manito", "Oas", "Pio Duran", "Polangui", "Rapu-Rapu",
                "Santo Domingo", "Tabaco City", "Tiwi"),
            "Antique" to listOf("Anini-y", "Barbaza", "Belison", "Bugasong", "Caluya",
                "Culasi", "Hamtic", "Laua-an", "Libertad", "Pandan",
                "Patnongon", "San Jose de Buenavista", "San Remigio",
                "Sebaste", "Sibalom", "Tibiao", "Tobias Fornier", "Valderrama"),
            "Aurora" to listOf("Baler", "Casiguran", "Dilasag", "Dinalungan",
                "Dingalan", "Dipaculao", "Maria Aurora", "San Luis"),
            "Bataan" to listOf("Abucay", "Bagac", "Balanga City", "Dinalupihan",
                "Hermosa", "Limay", "Mariveles", "Morong", "Orani",
                "Orion", "Pilar", "Samal"),
            "Batangas" to listOf("Agoncillo", "Alitagtag", "Balayan", "Balete",
                "Batangas City", "Bauan", "Calaca", "Calatagan", "Cuenca",
                "Ibaan", "Laurel", "Lemery", "Lian", "Lipa City", "Lobo",
                "Mabini", "Malvar", "Nasugbu", "Rosario", "San Jose",
                "San Juan", "Santa Teresita", "Santo Tomas", "Taal",
                "Talisay", "Tanauan City", "Taysan", "Tingloy", "Tuy"),
            "Benguet" to listOf("Atok", "Baguio City", "Bakun", "Bokod", "Buguias",
                "Itogon", "Kabayan", "Kapangan", "Kibungan", "La Trinidad",
                "Mankayan", "Sablan", "Tuba", "Tublay"),
            "Bohol" to listOf("Alburquerque", "Alicia", "Anda", "Antequera", "Baclayon",
                "Balilihan", "Batuan", "Bilar", "Buenavista", "Calape",
                "Candijay", "Carmen", "Clarin", "Corella", "Cortes",
                "Dagohoy", "Danao", "Dauis", "Dimiao", "Duero",
                "Getafe", "Guindulman", "Inabanga", "Jagna", "Lila",
                "Loay", "Loboc", "Loon", "Mabini", "Maribojoc",
                "Panglao", "Pilar", "Sagbayan", "San Isidro", "San Miguel",
                "Sevilla", "Sierra Bullones", "Tagbilaran City", "Talibon",
                "Trinidad", "Tubigon", "Ubay", "Valencia"),
            "Bukidnon" to listOf("Baungon", "Cabanglasan", "Damulog", "Dangcagan",
                "Don Carlos", "Impasug-Ong", "Kadingilan", "Kalilangan",
                "Kibawe", "Kitaotao", "Lantapan", "Libona", "Malitbog",
                "Manolo Fortich", "Maramag", "Quezon", "San Fernando",
                "Sumilao", "Talakag", "Valencia City"),
            "Bulacan" to listOf("Angat", "Balagtas", "Baliuag", "Bocaue", "Bulakan",
                "Bustos", "Calumpit", "Guiguinto", "Hagonoy", "Marilao",
                "Meycauayan City", "Norzagaray", "Obando", "Pandi",
                "Paombong", "Plaridel", "Pulilan", "San Ildefonso",
                "San Jose del Monte City", "San Miguel", "San Rafael",
                "Santa Maria"),
            "Cagayan" to listOf("Abulug", "Alcala", "Allacapan", "Amulung", "Aparri",
                "Baggao", "Ballesteros", "Buguey", "Camalaniugan", "Claveria",
                "Enrile", "Gattaran", "Gonzaga", "Iguig", "Lal-lo",
                "Lasam", "Pamplona", "Peñablanca", "Piat", "Rizal",
                "Sanchez-Mira", "Santa Ana", "Santa Teresita", "Santo Niño",
                "Solana", "Tuao", "Tuguegarao City"),
            "Camarines Norte" to listOf("Basud", "Capalonga", "Daet", "Jose Panganiban",
                "Labo", "Mercedes", "Paracale", "San Vicente",
                "Santa Elena", "Talisay", "Vinzons"),
            "Camarines Sur" to listOf("Baao", "Balatan", "Bato", "Bombon", "Buhi",
                "Bula", "Cabusao", "Calabanga", "Camaligan", "Canaman",
                "Caramoan", "Del Gallego", "Gainza", "Garchitorena",
                "Goa", "Iriga City", "Lagonoy", "Libmanan", "Lupi",
                "Magarao", "Milaor", "Minalabac", "Nabua", "Naga City",
                "Ocampo", "Pamplona", "Pasacao", "Pili", "Ragay",
                "San Fernando", "San Jose", "Sipocot", "Siruma",
                "Tigaon", "Tinambac"),
            "Cavite" to listOf("Alfonso", "Amadeo", "Bacoor City", "Carmona",
                "Cavite City", "Dasmariñas City", "General Trias City",
                "Imus City", "Indang", "Kawit", "Magallanes", "Maragondon",
                "Mendez", "Naic", "Noveleta", "Rosario", "Silang",
                "Tagaytay City", "Tanza", "Ternate", "Trece Martires City"),
            "Cebu" to listOf("Alcoy", "Alegria", "Aloguinsan", "Argao", "Asturias",
                "Badian", "Balamban", "Bantayan", "Barili", "Bogo City",
                "Boljoon", "Borbon", "Carcar City", "Carmen", "Catmon",
                "Cebu City", "Compostela", "Consolacion", "Cordova",
                "Daanbantayan", "Dalaguete", "Danao City", "Dumanjug",
                "Ginatilan", "Lapu-Lapu City", "Liloan", "Madridejos",
                "Malabuyoc", "Mandaue City", "Medellin", "Minglanilla",
                "Moalboal", "Naga City", "Oslob", "Pinamungajan",
                "Poro", "Ronda", "Samboan", "San Fernando", "San Francisco",
                "San Remigio", "Santa Fe", "Santander", "Sibonga",
                "Sogod", "Tabogon", "Talisay City", "Toledo City",
                "Tuburan", "Tudela"),
            "Davao del Norte" to listOf("Asuncion", "Carmen", "Kapalong",
                "New Corella", "Panabo City", "San Isidro",
                "Santo Tomas", "Tagum City", "Talaingod"),
            "Davao del Sur" to listOf("Bansalan", "Davao City", "Digos City",
                "Hagonoy", "Kiblawan", "Magsaysay", "Malalag",
                "Matanao", "Padada", "Santa Cruz", "Sulop"),
            "Davao Oriental" to listOf("Baganga", "Banaybanay", "Boston", "Caraga",
                "Cateel", "Governor Generoso", "Lupon", "Manay",
                "Mati City", "San Isidro", "Tarragona"),
            "Eastern Samar" to listOf("Arteche", "Balangiga", "Balangkayan",
                "Borongan City", "Can-avid", "Dolores", "General MacArthur",
                "Giporlos", "Guiuan", "Hernani", "Jipapad", "Lawaan",
                "Llorente", "Maslog", "Maydolong", "Mercedes", "Oras",
                "Quinapondan", "Salcedo", "San Julian", "San Policarpo",
                "Sulat", "Taft"),
            "Ilocos Norte" to listOf("Adams", "Bacarra", "Badoc", "Bangui", "Banna",
                "Batac City", "Burgos", "Carasi", "Currimao", "Dingras",
                "Dumalneg", "Laoag City", "Marcos", "Nueva Era",
                "Pagudpud", "Paoay", "Pasuquin", "Piddig", "Pinili",
                "San Nicolas", "Sarrat", "Solsona", "Vintar"),
            "Ilocos Sur" to listOf("Alilem", "Banayoyo", "Bantay", "Burgos",
                "Cabugao", "Candon City", "Caoayan", "Cervantes",
                "Galimuyod", "Magsingal", "Narvacan", "Salcedo",
                "San Esteban", "San Juan", "San Vicente", "Santa",
                "Santa Catalina", "Santa Cruz", "Santa Lucia", "Santa Maria",
                "Santiago", "Santo Domingo", "Sinait", "Tagudin", "Vigan City"),
            "Iloilo" to listOf("Ajuy", "Alimodian", "Anilao", "Badiangan", "Balasan",
                "Banate", "Barotac Nuevo", "Barotac Viejo", "Batad",
                "Bingawan", "Cabatuan", "Calinog", "Carles", "Concepcion",
                "Dingle", "Dumangas", "Estancia", "Guimbal", "Igbaras",
                "Iloilo City", "Janiuay", "Lambunao", "Leganes", "Leon",
                "Maasin", "Miagao", "New Lucena", "Oton", "Passi City",
                "Pavia", "Pototan", "San Dionisio", "San Enrique",
                "San Joaquin", "San Miguel", "Santa Barbara", "Sara",
                "Tigbauan", "Tubungan", "Zarraga"),
            "Isabela" to listOf("Alicia", "Angadanan", "Aurora", "Benito Soliven",
                "Burgos", "Cabagan", "Cabatuan", "Cauayan City", "Cordon",
                "Echague", "Gamu", "Ilagan City", "Jones", "Luna",
                "Maconacon", "Mallig", "Naguilian", "Palanan", "Quezon",
                "Ramon", "Roxas", "San Agustin", "San Guillermo",
                "San Isidro", "San Manuel", "San Mariano", "San Mateo",
                "San Pablo", "Santa Maria", "Santiago City", "Santo Tomas",
                "Tumauini"),
            "La Union" to listOf("Agoo", "Aringay", "Bacnotan", "Bagulin", "Balaoan",
                "Bangar", "Bauang", "Burgos", "Caba", "Luna",
                "Naguilian", "Pugo", "Rosario", "San Fernando City",
                "San Gabriel", "San Juan", "Santo Tomas", "Santol",
                "Sudipen", "Tubao"),
            "Laguna" to listOf("Alaminos", "Bay", "Biñan City", "Cabuyao City",
                "Calamba City", "Calauan", "Cavinti", "Famy", "Kalayaan",
                "Liliw", "Los Baños", "Luisiana", "Lumban", "Mabitac",
                "Magdalena", "Majayjay", "Nagcarlan", "Paete", "Pagsanjan",
                "Pakil", "Pangil", "Pila", "Rizal", "San Pablo City",
                "San Pedro City", "Santa Cruz", "Santa Maria",
                "Santa Rosa City", "Siniloan", "Victoria"),
            "Leyte" to listOf("Abuyog", "Alangalang", "Albuera", "Babatngon",
                "Barugo", "Bato", "Baybay City", "Burauen", "Calubian",
                "Capoocan", "Carigara", "Dagami", "Dulag", "Hilongos",
                "Hindang", "Inopacan", "Isabel", "Jaro", "Javier",
                "Julita", "Kananga", "La Paz", "MacArthur", "Mahaplag",
                "Matag-ob", "Matalom", "Mayorga", "Merida", "Ormoc City",
                "Palo", "Palompon", "Pastrana", "San Isidro", "San Miguel",
                "Santa Fe", "Tabango", "Tacloban City", "Tanauan",
                "Tolosa", "Tunga", "Villaba"),
            "Marinduque" to listOf("Boac", "Buenavista", "Gasan", "Mogpog",
                "Santa Cruz", "Torrijos"),
            "Masbate" to listOf("Aroroy", "Baleno", "Balud", "Batuan", "Cataingan",
                "Cawayan", "Claveria", "Dimasalang", "Esperanza", "Mandaon",
                "Masbate City", "Milagros", "Mobo", "Monreal", "Palanas",
                "Placer", "San Fernando", "San Jacinto", "San Pascual", "Uson"),
            "Misamis Occidental" to listOf("Aloran", "Baliangao", "Bonifacio",
                "Calamba", "Clarin", "Concepcion", "Jimenez",
                "Lopez Jaena", "Oroquieta City", "Ozamiz City",
                "Panaon", "Plaridel", "Sapang Dalaga", "Sinacaban",
                "Tangub City", "Tudela"),
            "Misamis Oriental" to listOf("Alubijid", "Balingasag", "Balingoan",
                "Binuangan", "Cagayan de Oro City", "Claveria",
                "El Salvador City", "Gingoog City", "Gitagum",
                "Initao", "Jasaan", "Kinoguitan", "Lagonglong",
                "Laguindingan", "Libertad", "Lugait", "Magsaysay",
                "Manticao", "Medina", "Naawan", "Opol", "Salay",
                "Tagoloan", "Talisayan", "Villanueva"),
            "Mountain Province" to listOf("Barlig", "Bauko", "Besao", "Bontoc",
                "Kadaclan", "Natonin", "Paracelis", "Sabangan",
                "Sadanga", "Sagada", "Tadian"),
            "Negros Occidental" to listOf("Bacolod City", "Bago City", "Binalbagan",
                "Calatrava", "Candoni", "Cauayan", "Escalante City",
                "Himamaylan City", "Hinigaran", "Hinoba-an", "Ilog",
                "Isabela", "Kabankalan City", "La Carlota City",
                "La Castellana", "Manapla", "Moises Padilla", "Murcia",
                "Pontevedra", "Pulupandan", "Sagay City", "San Carlos City",
                "San Enrique", "Silay City", "Sipalay City",
                "Talisay City", "Toboso", "Valladolid", "Victorias City"),
            "Negros Oriental" to listOf("Amlan", "Ayungon", "Bacong", "Bais City",
                "Basay", "Bayawan City", "Bindoy", "Canlaon City",
                "Dauin", "Dumaguete City", "Guihulngan City", "Jimalalud",
                "La Libertad", "Mabinay", "Manjuyod", "Pamplona",
                "San Jose", "Santa Catalina", "Siaton", "Sibulan",
                "Tanjay City", "Tayasan", "Valencia", "Vallehermoso",
                "Zamboanguita"),
            "Nueva Ecija" to listOf("Aliaga", "Bongabon", "Cabanatuan City", "Cabiao",
                "Carranglan", "Cuyapo", "Gabaldon", "Gapan City",
                "General Tinio", "Guimba", "Jaen", "Laur", "Licab",
                "Llanera", "Lupao", "Muñoz City", "Nampicuan",
                "Palayan City", "Pantabangan", "Peñaranda", "Quezon",
                "Rizal", "San Antonio", "San Isidro", "San Jose City",
                "San Leonardo", "Santa Rosa", "Santo Domingo",
                "Talavera", "Talugtug", "Zaragoza"),
            "Nueva Vizcaya" to listOf("Alfonso Castañeda", "Ambaguio", "Aritao",
                "Bagabag", "Bambang", "Bayombong", "Diadi",
                "Dupax del Norte", "Dupax del Sur", "Kasibu",
                "Kayapa", "Quezon", "Santa Fe", "Solano", "Villaverde"),
            "Occidental Mindoro" to listOf("Abra de Ilog", "Calintaan", "Looc",
                "Lubang", "Magsaysay", "Mamburao", "Paluan",
                "Rizal", "Sablayan", "San Jose", "Santa Cruz"),
            "Oriental Mindoro" to listOf("Baco", "Bansud", "Bongabong", "Bulalacao",
                "Calapan City", "Gloria", "Mansalay", "Naujan",
                "Pinamalayan", "Pola", "Puerto Galera", "Roxas",
                "San Teodoro", "Socorro", "Victoria"),
            "Palawan" to listOf("Aborlan", "Agutaya", "Araceli", "Balabac",
                "Bataraza", "Brooke's Point", "Busuanga", "Cagayancillo",
                "Coron", "Culion", "Cuyo", "Dumaran", "El Nido",
                "Kalayaan", "Linapacan", "Magsaysay", "Narra",
                "Puerto Princesa City", "Quezon", "Rizal", "Roxas",
                "San Vicente", "Taytay"),
            "Pampanga" to listOf("Angeles City", "Apalit", "Arayat", "Bacolor",
                "Candaba", "Floridablanca", "Guagua", "Lubao",
                "Mabalacat City", "Macabebe", "Magalang", "Masantol",
                "Mexico", "Minalin", "Porac", "San Fernando City",
                "San Luis", "San Simon", "Santa Ana", "Santa Rita",
                "Santo Tomas", "Sasmuan"),
            "Pangasinan" to listOf("Agno", "Aguilar", "Alaminos City", "Alcala",
                "Anda", "Asingan", "Balungao", "Bani", "Basista",
                "Bautista", "Bayambang", "Binalonan", "Binmaley",
                "Bolinao", "Bugallon", "Burgos", "Calasiao",
                "Dagupan City", "Dasol", "Infanta", "Labrador",
                "Laoac", "Lingayen", "Mabini", "Malasiqui", "Manaoag",
                "Mangaldan", "Mangatarem", "Mapandan", "Natividad",
                "Pozorrubio", "Rosales", "San Carlos City", "San Fabian",
                "San Jacinto", "San Manuel", "San Nicolas", "San Quintin",
                "Santa Barbara", "Santa Maria", "Santo Tomas", "Sison",
                "Sual", "Tayug", "Umingan", "Urbiztondo",
                "Urdaneta City", "Villasis"),
            "Quezon" to listOf("Agdangan", "Alabat", "Atimonan", "Buenavista",
                "Burdeos", "Calauag", "Candelaria", "Catanauan",
                "Dolores", "General Luna", "General Nakar", "Guinayangan",
                "Gumaca", "Infanta", "Jomalig", "Lopez", "Lucban",
                "Lucena City", "Macalelon", "Mauban", "Mulanay",
                "Padre Burgos", "Pagbilao", "Panukulan", "Patnanungan",
                "Perez", "Pitogo", "Plaridel", "Polillo", "Quezon",
                "Real", "Sampaloc", "San Andres", "San Antonio",
                "San Francisco", "San Narciso", "Sariaya", "Tagkawayan",
                "Tayabas City", "Tiaong", "Unisan"),
            "Quirino" to listOf("Aglipay", "Cabarroguis", "Diffun", "Maddela",
                "Nagtipunan", "Saguday"),
            "Rizal" to listOf("Angono", "Antipolo City", "Baras", "Binangonan",
                "Cainta", "Cardona", "Jalajala", "Morong", "Pililla",
                "Rodriguez", "San Mateo", "Tanay", "Taytay", "Teresa"),
            "Samar" to listOf("Almagro", "Basey", "Calbayog City", "Calbiga",
                "Catbalogan City", "Daram", "Gandara", "Hinabangan",
                "Jiabong", "Marabut", "Matuguinao", "Motiong",
                "Paranas", "Pinabacdao", "San Jorge", "San Jose de Buan",
                "San Sebastian", "Santa Margarita", "Santa Rita",
                "Santo Niño", "Tagapul-an", "Talalora", "Tarangnan",
                "Villareal", "Wright", "Zumarraga"),
            "Sarangani" to listOf("Alabel", "Glan", "Kiamba", "Maasim",
                "Maitum", "Malapatan", "Malungon"),
            "Siquijor" to listOf("Enrique Villanueva", "Larena", "Lazi",
                "Maria", "San Juan", "Siquijor"),
            "Sorsogon" to listOf("Barcelona", "Bulan", "Bulusan", "Casiguran",
                "Castilla", "Donsol", "Gubat", "Irosin", "Juban",
                "Magallanes", "Matnog", "Pilar", "Prieto Diaz",
                "Santa Magdalena", "Sorsogon City"),
            "South Cotabato" to listOf("Banga", "General Santos City",
                "Koronadal City", "Lake Sebu", "Norala", "Polomolok",
                "Santo Niño", "Surallah", "T'Boli", "Tampakan",
                "Tantangan", "Tupi"),
            "Southern Leyte" to listOf("Anahawan", "Bontoc", "Hinunangan",
                "Hinundayan", "Libagon", "Liloan", "Limasawa",
                "Maasin City", "Macrohon", "Malitbog", "Padre Burgos",
                "Pintuyan", "Saint Bernard", "San Francisco",
                "San Juan", "San Ricardo", "Silago", "Sogod",
                "Tomas Oppus"),
            "Sultan Kudarat" to listOf("Bagumbayan", "Columbio", "Esperanza",
                "Isulan", "Kalamansig", "Lambayong", "Lebak",
                "Lutayan", "Palimbang", "President Quirino",
                "Tacurong City"),
            "Tarlac" to listOf("Anao", "Bamban", "Camiling", "Capas",
                "Concepcion", "Gerona", "La Paz", "Mayantoc",
                "Moncada", "Paniqui", "Pura", "Ramos", "San Clemente",
                "San Jose", "San Manuel", "Santa Ignacia",
                "Tarlac City", "Victoria"),
            "Zambales" to listOf("Botolan", "Cabangan", "Candelaria", "Castillejos",
                "Iba", "Masinloc", "Olongapo City", "Palauig",
                "San Antonio", "San Felipe", "San Marcelino",
                "San Narciso", "Santa Cruz", "Subic"),
            "Zamboanga del Norte" to listOf("Dapitan City", "Dipolog City",
                "Godod", "Gutalac", "Kalawit", "Katipunan",
                "La Libertad", "Labason", "Liloy", "Manukan",
                "Mutia", "Piñan", "Polanco", "Rizal", "Salug",
                "Siayan", "Sibuco", "Sibutad", "Sindangan",
                "Siocon", "Sirawai", "Tampilisan"),
            "Zamboanga del Sur" to listOf("Aurora", "Bayog", "Dimataling",
                "Dinas", "Dumalinao", "Dumingag", "Guipos",
                "Josefina", "Kumalarang", "Labangan", "Lakewood",
                "Lapuyan", "Mahayag", "Margosatubig", "Midsalip",
                "Molave", "Pagadian City", "Pitogo", "Ramon Magsaysay",
                "San Miguel", "San Pablo", "Tabina", "Tambulig",
                "Tigbao", "Tukuran", "Tungawan", "Zamboanga City"),
            "Zamboanga Sibugay" to listOf("Alicia", "Buug", "Diplahan",
                "Imelda", "Ipil", "Kabasalan", "Mabuhay",
                "Malangas", "Naga", "Olutanga", "Payao",
                "Roseller Lim", "Siay", "Talusan", "Titay", "Tungawan")
        ).flatMap { (province, cities) ->
            cities.map { city -> "$city, $province" }
        }.sorted()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isGoogleSignUp = arguments?.getBoolean("isGoogleSignUp", false) ?: false

        if (isGoogleSignUp) {
            binding.etFirstName.setText(arguments?.getString("firstName") ?: "")
            binding.etLastName.setText(arguments?.getString("lastName")   ?: "")
            binding.etEmail.setText(arguments?.getString("email")         ?: "")
            binding.etFirstName.isEnabled = false
            binding.etLastName.isEnabled  = false
            binding.etEmail.isEnabled     = false
            binding.btnNext.text          = "Complete Profile"
        } else {
            FirebaseAuth.getInstance().signOut()
            binding.etFirstName.isEnabled = true
            binding.etLastName.isEnabled  = true
            binding.etEmail.isEnabled     = true
            binding.btnNext.text          = "Next"
        }

        binding.layoutPassword.visibility        = View.GONE
        binding.layoutConfirmPassword.visibility = View.GONE

        // ✅ Setup place autocomplete
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            philippineLocations
        )
        binding.etPlace.setAdapter(adapter)
        binding.etPlace.setOnItemClickListener { _, _, _, _ ->
            binding.layoutPlace.error = null
        }

        setupClickListeners(isGoogleSignUp)
        observeViewModel(isGoogleSignUp)
    }

    private fun setupClickListeners(isGoogleSignUp: Boolean) {
        binding.etBirthdate.setOnClickListener { showDatePicker() }

        binding.btnNext.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName  = binding.etLastName.text.toString().trim()
            val email     = binding.etEmail.text.toString().trim()
            val birthdate = binding.etBirthdate.text.toString().trim()
            val school    = binding.etSchool.text.toString().trim()
            val place     = binding.etPlace.text.toString().trim()

            clearErrors()
            var hasError = false

            if (firstName.isBlank()) {
                binding.layoutFirstName.error = "First name is required"; hasError = true
            } else if (firstName.length < 2) {
                binding.layoutFirstName.error = "First name must be at least 2 characters"; hasError = true
            } else if (!firstName.matches(Regex("^[a-zA-Z ]+$"))) {
                binding.layoutFirstName.error = "First name must contain letters only"; hasError = true
            } else binding.layoutFirstName.error = null

            if (lastName.isBlank()) {
                binding.layoutLastName.error = "Last name is required"; hasError = true
            } else if (lastName.length < 2) {
                binding.layoutLastName.error = "Last name must be at least 2 characters"; hasError = true
            } else if (!lastName.matches(Regex("^[a-zA-Z ]+$"))) {
                binding.layoutLastName.error = "Last name must contain letters only"; hasError = true
            } else binding.layoutLastName.error = null

            if (email.isBlank()) {
                binding.layoutEmail.error = "Email is required"; hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.layoutEmail.error = "Please enter a valid email address"; hasError = true
            } else if (!email.endsWith("@gmail.com")) {
                binding.layoutEmail.error = "Only @gmail.com addresses are allowed"; hasError = true
            } else binding.layoutEmail.error = null

            if (birthdate.isBlank()) {
                binding.layoutBirthdate.error = "Birthdate is required"; hasError = true
            } else {
                val dateError = validateBirthdate(birthdate)
                if (dateError != null) {
                    binding.layoutBirthdate.error = dateError; hasError = true
                } else binding.layoutBirthdate.error = null
            }

            if (school.isBlank()) {
                binding.layoutSchool.error = "School is required"; hasError = true
            } else if (school.length < 3) {
                binding.layoutSchool.error = "Please enter a valid school name"; hasError = true
            } else binding.layoutSchool.error = null

            // ✅ Validate place — must be from the list
            if (place.isBlank()) {
                binding.layoutPlace.error = "Place is required"; hasError = true
            } else if (!philippineLocations.contains(place)) {
                binding.layoutPlace.error = "Please select a valid Philippine location"; hasError = true
            } else binding.layoutPlace.error = null

            if (hasError) return@setOnClickListener

            if (isGoogleSignUp) {
                viewModel.completeGoogleProfile(birthdate, school, place)
            } else {
                val bundle = Bundle().apply {
                    putString("firstName", firstName)
                    putString("lastName",  lastName)
                    putString("email",     email)
                    putString("birthdate", birthdate)
                    putString("school",    school)
                    putString("place",     place)
                }
                findNavController().navigate(
                    R.id.action_registerFragment_to_createPasswordFragment, bundle
                )
            }
        }

        binding.tvLogin.setOnClickListener { findNavController().popBackStack() }
    }

    private fun clearErrors() {
        binding.layoutFirstName.error = null
        binding.layoutLastName.error  = null
        binding.layoutEmail.error     = null
        binding.layoutBirthdate.error = null
        binding.layoutSchool.error    = null
        binding.layoutPlace.error     = null
    }

    private fun validateBirthdate(birthdate: String): String? {
        return try {
            val parts = birthdate.split("/")
            if (parts.size != 3) return "Invalid date format (MM/DD/YYYY)"
            val month = parts[0].toInt()
            val day   = parts[1].toInt()
            val year  = parts[2].toInt()
            if (month < 1 || month > 12) return "Invalid month"
            if (day < 1 || day > 31)     return "Invalid day"
            if (year < 1900)             return "Invalid birth year"
            val today    = java.util.Calendar.getInstance()
            val birthday = java.util.Calendar.getInstance().apply { set(year, month - 1, day) }
            if (birthday.after(today)) return "Birthdate cannot be in the future"
            var age = today.get(java.util.Calendar.YEAR) - birthday.get(java.util.Calendar.YEAR)
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthday.get(java.util.Calendar.DAY_OF_YEAR)) age--
            if (age < 13) return "You must be at least 13 years old to register"
            null
        } catch (e: NumberFormatException) { "Invalid date format" }
    }

    private fun showDatePicker() {
        val today = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                binding.etBirthdate.setText("${month + 1}/$day/$year")
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).also {
            it.datePicker.maxDate = today.timeInMillis
            it.datePicker.minDate = Calendar.getInstance().apply {
                set(1900, Calendar.JANUARY, 1)
            }.timeInMillis
            it.show()
        }
    }

    private fun observeViewModel(isGoogleSignUp: Boolean) {
        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegisterViewModel.RegisterState.Loading -> {
                    binding.btnNext.isEnabled      = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RegisterViewModel.RegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (isGoogleSignUp) {
                        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                    }
                    viewModel.resetState()
                }
                is RegisterViewModel.RegisterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnNext.isEnabled      = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                else -> {
                    binding.btnNext.isEnabled      = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}