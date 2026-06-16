# 🌤️ WeatherApp — Final Exam Project

თანამედროვე Android ამინდის აპლიკაცია მუქი ლურჯი დიზაინით, რეალური დროის მონაცემებით და GPS-ით.

---

## 📱 აპლიკაციის აღწერა

WeatherApp აჩვენებს:
- **მიმდინარე ამინდს** ნებისმიერი ქალაქისთვის: ტემპერატურა, შეგრძნება, ტენიანობა, ქარის სიჩქარე, აღწერა და ამინდის იკონი
- **7-დღიანი პროგნოზი** გადახვევადი სიის სახით მაქსიმალური/მინიმალური ტემპერატურით, აღწერით და წვიმის ალბათობით
- **GPS ავტო-განსაზღვრა** — აპლიკაცია ავტომატურად ადგენს შენს ქალაქს მოწყობილობის მდებარეობის გამოყენებით

მონაცემები იტვირთება **OpenWeatherMap API**-დან Retrofit-ის საშუალებით და **ინახება ლოკალურად Room-ში**, ასე რომ ბოლო შედეგი ინტერნეტის გარეშეც ჩანს.

---

## 🏗️ არქიტექტურა: MVVM

```
UI Layer (HomeFragment, ForecastFragment)
    │  observes LiveData
    ▼
WeatherViewModel
    │  calls repository
    ▼
WeatherRepository  ◄──────────────────────┐
    │                                      │
    ▼                                      ▼
Room Database                     Retrofit (OpenWeatherMap API)
(WeatherDatabase, WeatherDao,     (WeatherApiService,
 ForecastDao)                      RetrofitInstance)
```

| Layer | Classes | პასუხისმგებლობა |
|-------|---------|----------------|
| **Model** | `CurrentWeather`, `ForecastDay` | მონაცემთა კლასები + Room Entity-ები |
| **View** | `MainActivity`, `HomeFragment`, `ForecastFragment` | მხოლოდ UI, ბიზნეს ლოგიკის გარეშე |
| **ViewModel** | `WeatherViewModel` | LiveData, coroutines, მდგომარეობა |
| **Repository** | `WeatherRepository` | მონაცემთა ერთიანი წყარო |
| **Local DB** | `WeatherDatabase`, `WeatherDao`, `ForecastDao` | Room/SQLite ქეში |
| **Remote** | `WeatherApiService`, `RetrofitInstance` | HTTP Retrofit-ით |

---

## 🔧 ტექნიკური დეტალები

### ბიბლიოთეკები

| Library | დანიშნულება |
|---------|---------|
| **Room 2.6.1** | ამინდის მონაცემების ლოკალური SQLite ქეში |
| **Retrofit 2.9.0** | REST მოთხოვნები OpenWeatherMap API-სთან |
| **Glide 4.16.0** | ამინდის იკონების ჩატვირთვა CDN URL-იდან |
| **Navigation Component 2.7.7** | Fragment-ების ნავიგაცია + back stack |
| **LiveData + ViewModel 2.7.0** | MVVM რეაქტიული მონაცემთა ფენა |
| **Coroutines 1.7.3** | ფონური ნაკადები ქსელისა და DB-სთვის |
| **ViewBinding** | View-ებზე წვდომა findViewByID-ის გარეშე |
| **FusedLocationProviderClient** | ⭐ GPS მდებარეობა — ახალი ფუნქციონალი |

### ⭐ ახალი ფუნქციონალი: GPS მდებარეობა (FusedLocationProviderClient)

წინა ლექციებში არ გამოგვიყენებია. აპლიკაცია იყენებს Google Play Services-ის `FusedLocationProviderClient`-ს:
1. ითხოვს `ACCESS_FINE_LOCATION` უფლებას runtime-ზე `ActivityResultContracts`-ის გამოყენებით
2. იძახებს `fusedLocationClient.lastLocation`-ს მოწყობილობის კოორდინატების მისაღებად
3. lat/lon გადაეცემა ViewModel → Repository → Retrofit API-ს
4. API აბრუნებს ქალაქის სახელს და ამინდს იმ კოორდინატებისთვის

### მონაცემთა ბაზის ცხრილები

**current_weather** — ინახავს ბოლოს მოძიებულ ამინდს ქალაქისთვის

| Column | Type | შენიშვნა |
|--------|------|-------|
| cityName | TEXT (PK) | ერთი ჩანაწერი ქალაქზე |
| temperature | REAL | °C |
| feelsLike | REAL | °C |
| humidity | INTEGER | % |
| windSpeed | REAL | მ/წმ |
| description | TEXT | მაგ. "thunderstorm" |
| iconCode | TEXT | მაგ. "11d" — Glide იყენებს იკონის ჩასატვირთად |
| country | TEXT | ქვეყნის კოდი მაგ. "GE" |
| timestamp | INTEGER | Unix ms, მონაცემების მიღების დრო |

**forecast** — ინახავს 7-დღიანი პროგნოზის სტრიქონებს ქალაქისთვის

| Column | Type | შენიშვნა |
|--------|------|-------|
| id | INTEGER (PK) | Room-ის მიერ ავტო-გენერირებული |
| cityName | TEXT | current_weather-ის ქალაქთან კავშირი |
| date | TEXT | დღის სახელი მაგ. "Mon", "Tue" |
| tempMax | REAL | დღის მაქსიმუმი °C |
| tempMin | REAL | დღის მინიმუმი °C |
| description | TEXT | მაგ. "light rain" |
| iconCode | TEXT | Glide-ს იყენებს იკონისთვის |
| humidity | INTEGER | % |
| windSpeed | REAL | მ/წმ |
| chanceOfRain | INTEGER | 0–100% |

---

## ✅ საგამოცდო მოთხოვნები

- [x] **მენიუ** — Toolbar Search (🔍) და Refresh (🔄) ღილაკებით
- [x] **სია** — RecyclerView 7-დღიანი პროგნოზით, `ListAdapter` + `DiffUtil`
- [x] **MVVM არქიტექტურა** — `WeatherViewModel` + `WeatherRepository` + `LiveData`
- [x] **ბაზასთან კავშირი** — Room (ლოკალური ქეში) + Retrofit (OpenWeatherMap API)
- [x] **ახალი ფუნქციონალი** — GPS მდებარეობა `FusedLocationProviderClient`-ით
- [x] **README** — ეს ფაილი
- [x] **`findViewById` არ გამოიყენება** — ViewBinding ყველგან
- [x] **XML-ში `Fragment` ტეგი არ გამოიყენება** — გამოიყენება `FragmentContainerView`
