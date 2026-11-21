/**
 * AndyTV - Módulo de Guía de Programación Electrónica (EPG)
 * Proporciona información en tiempo real de "Ahora" y "A continuación"
 * con cálculo dinámico de tiempos y progreso.
 */

(function () {
  const root = typeof window !== 'undefined' ? window : (typeof global !== 'undefined' ? global : this);

  // Grilla horaria estándar de televisión argentina (días hábiles y fines de semana)
  // Formato: [horaInicio (decimal, ej 13.5 = 13:30), horaFin, "Título del programa"]
  const SCHEDULES_WEEKDAY = {
    tn: [
      [0.0, 6.0, "Re Despiertos / TN Madrugada"],
      [6.0, 10.0, "TN de 6 a 10"],
      [10.0, 13.0, "TN de 10 a 13"],
      [13.0, 14.5, "Mediodía Noticias"],
      [14.5, 17.0, "Está Pasando"],
      [17.0, 20.0, "Nuestra Tarde"],
      [20.0, 22.0, "TN Central"],
      [22.0, 23.0, "Solo Una Vuelta Más"],
      [23.0, 24.0, "Verdad Consecuencia / TN Noche"]
    ],
    telefe: [
      [0.0, 7.0, "Staff Telefe / Trasnoche"],
      [7.0, 9.5, "Buen Telefe"],
      [9.5, 11.5, "A la Barbarossa"],
      [11.5, 13.0, "Ariel en su Salsa"],
      [13.0, 14.5, "El Noticiero de la Gente"],
      [14.5, 17.0, "Cortá por Lozano"],
      [17.0, 20.0, "Pasión de Gavilanes / Espacio Novelas"],
      [20.0, 21.45, "Telefe Noticias"],
      [21.45, 23.3, "Prime Time: Escape Perfecto / Gran Hermano"],
      [23.3, 24.0, "Staff de Noticias"]
    ],
    c5n: [
      [0.0, 6.5, "Trasnoche C5N"],
      [6.5, 10.0, "Mañanas Argentinas"],
      [10.0, 13.0, "Nos Vemos"],
      [13.0, 16.0, "De Una"],
      [16.0, 18.0, "La Tarde x C5N"],
      [18.0, 20.0, "El Diario"],
      [20.0, 21.5, "Minuto Uno (M1)"],
      [21.5, 23.5, "Duro de Domar"],
      [23.5, 24.0, "La Voz de la Calle"]
    ],
    eltrece: [
      [0.0, 7.0, "En Síntesis / Noche en El Trece"],
      [7.0, 9.0, "Arriba Argentinos"],
      [9.0, 10.5, "Mañanísima"],
      [10.5, 13.0, "Socios del Espectáculo"],
      [13.0, 14.5, "Mediodía Noticias"],
      [14.5, 16.0, "100 Argentinos Dicen"],
      [16.0, 18.0, "Cuestión de Peso / Poco Correctos"],
      [18.0, 20.0, "Ahora Caigo"],
      [20.0, 21.5, "Telenoche"],
      [21.5, 23.5, "Los 8 Escalones"],
      [23.5, 24.0, "En Síntesis"]
    ],
    elnueve: [
      [0.0, 7.0, "Trasnoche 9"],
      [7.0, 10.0, "Telenueve al Amanecer"],
      [10.0, 12.0, "Qué Mañana!"],
      [12.0, 14.0, "Telenueve al Mediodía"],
      [14.0, 16.0, "Todas las Tardes"],
      [16.0, 17.5, "Médico de Familia"],
      [17.5, 19.0, "Escuela de Cocina"],
      [19.0, 20.3, "Telenueve Central"],
      [20.3, 22.0, "Bendita"],
      [22.0, 23.0, "Mejor de Noche"],
      [23.0, 24.0, "Telenueve al Cierre"]
    ],
    america: [
      [0.0, 6.0, "Trasnoche América"],
      [6.0, 10.0, "Buenos Días América (BDA)"],
      [10.0, 12.5, "Desayuno Americano"],
      [12.5, 15.0, "Intrusos en el Espectáculo"],
      [15.0, 18.0, "DDM (El Diario de Mariana)"],
      [18.0, 20.0, "América Noticias"],
      [20.0, 22.0, "LAM (Los Ángeles de la Mañana)"],
      [22.0, 23.5, "Noche al Dente"],
      [23.5, 24.0, "Pasó en América"]
    ],
    a24: [
      [0.0, 6.0, "Madrugada A24"],
      [6.0, 10.0, "A24 Primera Mañana"],
      [10.0, 13.0, "Vivo el Día"],
      [13.0, 15.0, "El Noticiero de A24"],
      [15.0, 18.0, "Tarde a Tarde"],
      [18.0, 21.0, "El Noticiero de la Tarde"],
      [21.0, 23.0, "Basta Baby"],
      [23.0, 24.0, "Último Momento A24"]
    ],
    tvpublica: [
      [0.0, 6.0, "Trasnoche Televisión Pública"],
      [6.0, 8.5, "Televisión Pública Noticias Mañana"],
      [8.5, 12.0, "Cocineros y Sabores de la Patria"],
      [12.0, 14.0, "Televisión Pública Noticias Mediodía"],
      [14.0, 16.5, "Cultura y Federalismo"],
      [16.5, 19.0, "Festival País / Deportes TVP"],
      [19.0, 21.0, "Televisión Pública Noticias Central"],
      [21.0, 23.0, "Documentales y Series Nacionales"],
      [23.0, 24.0, "Resumen Federal de Noticias"]
    ],
    lnmas: [
      [0.0, 6.0, "LN+ Madrugada"],
      [6.0, 10.0, "8 AM Noticias"],
      [10.0, 13.0, "Buen Día Nación"],
      [13.0, 15.0, "LN+ Mediodía"],
      [15.0, 18.0, "Hora 17"],
      [18.0, 20.0, "El Noticiero de LN+"],
      [20.0, 22.0, "+Realidad"],
      [22.0, 23.5, "+Nación"],
      [23.5, 24.0, "Mirá lo que te digo"]
    ],
    cronica: [
      [0.0, 6.0, "Crónica Madrugada / El Pueblo Quiere Saber"],
      [6.0, 9.5, "Crónica Matinal"],
      [9.5, 12.0, "Tiempo Real"],
      [12.0, 14.0, "Es Muy Temprano"],
      [14.0, 17.0, "La Tarde de Crónica"],
      [17.0, 20.0, "Siempre Noticias"],
      [20.0, 22.0, "Crónica Central en Vivo"],
      [22.0, 24.0, "Firme Junto al Pueblo"]
    ],
    canalrural: [
      [0.0, 6.0, "Trasnoche de Campo"],
      [6.0, 9.0, "Apertura de Mercados y Clima"],
      [9.0, 13.0, "Actualidad Agropecuaria y Remates"],
      [13.0, 15.0, "Noticiero Rural Mediodía"],
      [15.0, 19.0, "Tecnología, Genética y Cosechas"],
      [19.0, 21.0, "Cierre de Mercados Ganaderos y Granos"],
      [21.0, 24.0, "Especiales del Campo Argentino"]
    ],
    canal26: [
      [0.0, 6.0, "Canal 26 Trasnoche Internacional"],
      [6.0, 10.0, "26 Mañana"],
      [10.0, 13.0, "La Mañana de Canal 26"],
      [13.0, 15.0, "26 Noticias Mediodía"],
      [15.0, 18.0, "26 Tarde en Vivo"],
      [18.0, 20.0, "El Diario de Canal 26"],
      [20.0, 22.0, "26 Noticias Central"],
      [22.0, 24.0, "26 Global / Resumen del Mundo"]
    ],
    fifaplus: [
      [0.0, 6.0, "FIFA+ Archivo Histórico Mundial"],
      [6.0, 9.0, "Partidos Legendarios de Copas del Mundo"],
      [9.0, 12.0, "FIFA+ Documentales y Leyendas del Fútbol"],
      [12.0, 15.0, "Finales Históricas de la Copa Mundial"],
      [15.0, 18.0, "Mundiales de la FIFA: Los Mejores Goles"],
      [18.0, 21.0, "FIFA+ Magazine y Fútbol Internacional"],
      [21.0, 24.0, "Copa del Mundo FIFA: Edición de Oro"]
    ]
  };

  function decimalToTimeStr(dec) {
    let hours = Math.floor(dec);
    let mins = Math.round((dec - hours) * 60);
    if (hours >= 24) hours = hours % 24;
    const hh = String(hours).padStart(2, '0');
    const mm = String(mins).padStart(2, '0');
    return `${hh}:${mm}`;
  }

  function getChannelEpg(channelId, customDate) {
    const now = customDate || new Date();
    // Hora decimal local: ej 14:45 -> 14.75
    const currentDec = now.getHours() + now.getMinutes() / 60 + now.getSeconds() / 3600;

    const list = SCHEDULES_WEEKDAY[channelId] || [
      [0.0, 6.0, "Trasnoche en Vivo"],
      [6.0, 12.0, "Programación de la Mañana"],
      [12.0, 18.0, "Transmisión en Vivo de la Tarde"],
      [18.0, 24.0, "Edición Central en Vivo"]
    ];

    let currentProg = null;
    let nextProg = null;

    for (let i = 0; i < list.length; i++) {
      const [start, end, title] = list[i];
      if (currentDec >= start && currentDec < end) {
        const startStr = decimalToTimeStr(start);
        const endStr = decimalToTimeStr(end);
        const durationMin = (end - start) * 60;
        const elapsedMin = (currentDec - start) * 60;
        const progressPercent = Math.min(100, Math.max(0, Math.round((elapsedMin / durationMin) * 100)));

        currentProg = {
          title: title,
          start: startStr,
          end: endStr,
          progress: progressPercent
        };

        const nextEntry = list[i + 1] || list[0];
        nextProg = {
          title: nextEntry[2],
          start: decimalToTimeStr(nextEntry[0]),
          end: decimalToTimeStr(nextEntry[1])
        };
        break;
      }
    }

    if (!currentProg) {
      currentProg = {
        title: "Transmisión en Vivo",
        start: "En vivo",
        end: "",
        progress: 50
      };
      nextProg = {
        title: "Programación Regular",
        start: "",
        end: ""
      };
    }

    return {
      now: currentProg,
      next: nextProg
    };
  }

  root.AndyEpg = {
    getChannelEpg: getChannelEpg
  };
})();
