// Сборка презентации по лабораторной работе № 4 (pptxgenjs)
const path = require('path');
const fs = require('fs');
const pptxgen = require('pptxgenjs');

const PROJECT = '/Users/azatminyazov/Workspace/university java/4 work';
const OUT = process.env.OUT || path.join(PROJECT, 'docs/presentation/Презентация_ЛР4_Минязов.pptx');
const shot = n => path.join(PROJECT, 'docs/screenshots', n);
const uml = n => path.join(PROJECT, 'docs/uml', n);

// Палитра окна программы
const C = {
  dark: '2C2C2A', ink: '2C2C2A', muted: '6B6A65', light: 'F7F6F3', line: 'E4E2DC',
  path: '1D9E75', pathBg: 'E1F5EE', water: '185FA5', waterBg: 'E6F1FB', okText: '085041',
  shock: 'A32D2D', shockBg: 'FCEBEB', cheese: 'BA7517', cheeseBg: 'FAEEDA', white: 'FFFFFF',
};
const HEAD = 'Cambria';
const BODY = 'Calibri';

function pngSize(file) {
  const b = fs.readFileSync(file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

/** Картинка, вписанная в прямоугольник с сохранением пропорций (по центру). */
function fit(slide, file, x, y, w, h, extra = {}) {
  const s = pngSize(file);
  const k = Math.min(w / s.w, h / s.h);
  const iw = s.w * k, ih = s.h * k;
  slide.addImage({ path: file, x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih, ...extra });
  return { x: x + (w - iw) / 2, y: y + (h - ih) / 2, w: iw, h: ih };
}

function title(slide, text, sub) {
  slide.addText(text, { x: 0.6, y: 0.4, w: 12.1, h: 0.8, fontFace: HEAD, fontSize: 36, bold: true, color: C.ink, margin: 0, isTextBox: true });
  if (sub) {
    slide.addText(sub, { x: 0.6, y: 1.15, w: 12.1, h: 0.45, fontFace: BODY, fontSize: 16, color: C.muted, margin: 0, isTextBox: true });
  }
}

function card(slide, x, y, w, h, fill) {
  slide.addShape('roundRect', { x, y, w, h, fill: { color: fill }, line: { color: fill }, rectRadius: 0.12 });
}

/** Кружок с цифрой или знаком — общий мотив всех слайдов. */
function badge(slide, x, y, d, fill, text, color = C.white, size = 16) {
  slide.addShape('ellipse', { x, y, w: d, h: d, fill: { color: fill }, line: { color: fill } });
  slide.addText(text, { x, y, w: d, h: d, align: 'center', valign: 'middle', fontFace: BODY, fontSize: size, bold: true, color, margin: 0, isTextBox: true });
}

// цвета ролей, как в окне: феромон, лучший маршрут, муравей
const PHER = C.cheese, BEST = C.path, ANT = 'D85A30';

const pres = new pptxgen();
pres.layout = 'LAYOUT_WIDE';  // 13.33 x 7.5
pres.author = 'А. А. Минязов';
pres.title = 'Алгоритм муравьёв — оптимизация муравьиной колонией';

const bullets = (slide, items, box, size = 17) => slide.addText(
  items.map((t, i) => ({ text: t, options: { bullet: true, breakLine: i < items.length - 1 } })),
  { ...box, fontFace: BODY, fontSize: size, color: C.ink, paraSpaceAfter: 10, margin: 0, valign: 'top', isTextBox: true });


// ---------- 1. Титул ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Лабораторная работа № 4 · ООП на языке Java', { x: 0.6, y: 0.7, w: 6.4, h: 0.4, fontFace: BODY, fontSize: 16, color: 'B4B2A9', margin: 0, isTextBox: true });
  s.addText('Алгоритм муравьёв', { x: 0.6, y: 1.5, w: 6.4, h: 1.4, fontFace: HEAD, fontSize: 40, bold: true, color: C.white, margin: 0, isTextBox: true });
  s.addText('Муравьиная колония находит кратчайший маршрут коммивояжёра через феромон на путях', { x: 0.6, y: 3.0, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 20, color: 'D3D1C7', margin: 0, isTextBox: true });
  s.addText([
    { text: 'Выполнил: ', options: { color: 'B4B2A9' } }, { text: 'А. А. Минязов, гр. 5130902/40202', options: { color: C.white, breakLine: true } },
    { text: 'Проверил: ', options: { color: 'B4B2A9' } }, { text: 'ст. преп. Д. С. Хасанов', options: { color: C.white, breakLine: true } },
    { text: 'СПбПУ, ИКНК · 2026', options: { color: 'B4B2A9' } },
  ], { x: 0.6, y: 5.4, w: 6.2, h: 1.3, fontFace: BODY, fontSize: 16, margin: 0, paraSpaceAfter: 4, isTextBox: true });
  fit(s, shot('gui_09_random_end.png'), 7.2, 0.9, 5.6, 5.7);
  s.addNotes('Тема: муравьиная система Дориго для задачи коммивояжёра. Java, окно на JavaFX.');
}

// ---------- 2. Задача ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Задача', 'Найти кратчайший замкнутый маршрут через все города — задача коммивояжёра');
  bullets(s, [
    'Среда — двунаправленный граф: узел — город, ребро — путь, вес — расстояние.',
    'Муравьи слепы, но оставляют на путях феромон.',
    'Сначала муравьи бегают по всем дорогам почти наугад.',
    'После многих циклов вся колония идёт по кратчайшему маршруту.',
    'Классы из задания: муравей (перемещение, выбор пути), узел, путь.',
  ], { x: 0.6, y: 1.9, w: 5.8, h: 4.8 });
  fit(s, shot('gui_01_start.png'), 6.7, 1.8, 6.1, 5.0);
  s.addNotes('Пример из задания — шестиугольник: 6 городов, путь между каждой парой, 15 путей. Оптимум — периметр.');
}

// ---------- 3. Выбор пути ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Как муравей выбирает путь', 'Из города r — в ещё не посещённый город u; когда все посещены — домой');
  card(s, 0.6, 1.9, 12.1, 1.2, C.cheeseBg);
  s.addText('P(r, u) = τ(r, u)^α · (1 / d(r, u))^β  /  Σ τ^α · (1 / d)^β', { x: 0.9, y: 2.0, w: 11.5, h: 1.0, fontFace: 'Cambria Math', fontSize: 26, color: '633806', margin: 0, valign: 'middle', isTextBox: true });
  const items = [
    ['τ', 'феромон на пути', PHER],
    ['d', 'длина пути', C.muted],
    ['α', 'вес феромона', PHER],
    ['β', 'вес расстояния', C.muted],
  ];
  items.forEach(([k, d, col], i) => {
    badge(s, 0.7, 3.5 + i * 0.75, 0.55, col, k, C.white, 18);
    s.addText(d, { x: 1.45, y: 3.5 + i * 0.75, w: 4.2, h: 0.55, fontFace: BODY, fontSize: 18, color: C.ink, margin: 0, valign: 'middle', isTextBox: true });
  });
  card(s, 6.4, 3.4, 6.3, 3.3, C.light);
  s.addText([
    { text: 'Пример: пути длиной 5 и 3, феромон одинаковый', options: { bold: true, breakLine: true } },
    { text: 'P = (1/5) / (1/5 + 1/3) = 3/8 и 5/8', options: { breakLine: true } },
    { text: ' ', options: { breakLine: true } },
    { text: 'Феромона на длинном вдвое больше, α = 3:', options: { bold: true, breakLine: true } },
    { text: 'вес ×2³ = ×8 → вероятность 0,83', options: { breakLine: true } },
    { text: ' ', options: { breakLine: true } },
    { text: 'Выбор — рулетка: случайное r ∈ [0, 1) и накопленные P', options: { color: C.muted } },
  ], { x: 6.7, y: 3.6, w: 5.8, h: 3.0, fontFace: BODY, fontSize: 17, color: C.ink, margin: 0, valign: 'top', isTextBox: true });
  s.addNotes('Методы Ant.probabilities и Ant.chooseNext. Если феромон ушёл в ноль double, пути равновероятны.');
}

// ---------- 4. Феромон ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Феромон после цикла', 'Цикл — все муравьи обошли города и вернулись домой');
  const f = [
    ['Δτ = Q / L', 'каждый муравей на свой маршрут длины L'],
    ['τ = τ + ρ · ΣΔτ', 'нанесение — как в примере задания'],
    ['τ = τ · (1 − ρ)', 'испарение'],
  ];
  f.forEach(([a, b], i) => {
    card(s, 0.6, 1.9 + i * 1.0, 5.6, 0.85, C.cheeseBg);
    s.addText(a, { x: 0.85, y: 1.9 + i * 1.0, w: 2.7, h: 0.85, fontFace: 'Cambria Math', fontSize: 20, color: '633806', margin: 0, valign: 'middle', isTextBox: true });
    s.addText(b, { x: 3.5, y: 1.9 + i * 1.0, w: 2.6, h: 0.85, fontFace: BODY, fontSize: 14, color: C.ink, margin: 0, valign: 'middle', isTextBox: true });
  });
  s.addText('Пример «два муравья»: ρ 0,6, α 3, β 1, Q 10, τ₀ 0,1', { x: 6.6, y: 1.9, w: 6.2, h: 0.5, fontFace: BODY, fontSize: 18, bold: true, color: C.ink, margin: 0, isTextBox: true });
  s.addTable([
    [{ text: 'Путь', options: { bold: true, fill: { color: C.light } } }, { text: 'Δτ', options: { bold: true, fill: { color: C.light } } },
      { text: 'нанесение', options: { bold: true, fill: { color: C.light } } }, { text: 'испарение', options: { bold: true, fill: { color: C.light } } }],
    ['L = 20', '0,5', '0,1 + 0,6·0,5 = 0,4', '0,16'],
    ['L = 10', '1,0', '0,1 + 0,6·1,0 = 0,7', '0,28'],
  ], { x: 6.6, y: 2.6, w: 6.1, colW: [1.0, 0.8, 2.6, 1.7], fontFace: BODY, fontSize: 16, color: C.ink, border: { type: 'solid', color: C.line, pt: 1 }, rowH: 0.5 });
  s.addText('Совпадает с заданием до цифры. На коротком пути феромона почти вдвое больше — в следующем цикле его выберут в 10,7 раза чаще.', { x: 6.6, y: 4.4, w: 6.1, h: 1.2, fontFace: BODY, fontSize: 16, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('В тексте задания формула записана иначе, но числа примера посчитаны как τ + ρ·Δτ; программа следует числам. Методы Colony.deposit и Colony.evaporate.');
}

// ---------- 5. Сначала наугад, потом по кратчайшему ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Сначала наугад — потом все по кратчайшему', 'Шестиугольник из задания, 12 муравьёв');
  fit(s, shot('gui_02_cycle1.png'), 0.5, 1.8, 6.1, 4.1);
  fit(s, shot('gui_04_hex_end.png'), 6.75, 1.8, 6.1, 4.1);
  s.addText('Цикл 1: все 15 дорог, на одном маршруте 8 %, лучший 209', { x: 0.5, y: 6.0, w: 6.1, h: 0.6, align: 'center', fontFace: BODY, fontSize: 16, color: C.ink, margin: 0, isTextBox: true });
  s.addText('В конце: 6 дорог — периметр 168, на нём 100 %', { x: 6.75, y: 6.0, w: 6.1, h: 0.6, align: 'center', fontFace: BODY, fontSize: 16, bold: true, color: BEST, margin: 0, isTextBox: true });
  s.addNotes('Это два наблюдения, которых требует задание. Проверены тестами на 20 запусках.');
}

// ---------- 6. Элитные муравьи ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Элитные муравьи', 'Чистая муравьиная система находит лучший маршрут, но колония не всегда к нему сходится');
  bullets(s, [
    'Лучший маршрут нашёл один муравей — его феромона не хватает, колония закрепляется на маршруте чуть длиннее.',
    'Расширение Дориго: каждый цикл лучший найденный маршрут получает ещё e · Q / L.',
    'e = 0 — алгоритм ровно как в задании; кнопка «Как в задании».',
  ], { x: 0.6, y: 1.9, w: 5.8, h: 4.0 });
  s.addTable([
    [{ text: 'Вся колония на оптимуме', options: { bold: true, fill: { color: C.light } } },
      { text: '8 городов', options: { bold: true, fill: { color: C.light } } }, { text: '10 городов', options: { bold: true, fill: { color: C.light } } }],
    ['задание: α 3, ρ 0,6, e 0', '5 из 20', '6 из 20'],
    [{ text: 'по умолчанию: α 2, ρ 0,1, e 5', options: { bold: true, color: BEST } }, { text: '29 из 30', options: { bold: true, color: BEST } }, { text: '27 из 30', options: { bold: true, color: BEST } }],
  ], { x: 6.7, y: 2.0, w: 6.0, colW: [3.2, 1.4, 1.4], fontFace: BODY, fontSize: 16, color: C.ink, border: { type: 'solid', color: C.line, pt: 1 }, rowH: 0.6 });
  s.addText('Оптимум — полным перебором (до 10 городов, 9! = 362 880 порядков).', { x: 6.7, y: 4.2, w: 6.0, h: 0.8, fontFace: BODY, fontSize: 15, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('Параметры по умолчанию подобраны замером. Колония сходится в среднем за 29 циклов.');
}

// ---------- 7. Классы ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Классы', 'Модуль aco: модель, колония, управление, окно');
  const cols = [
    ['aco.model', 'Node — узел (город)\nPath — путь: длина, феромон\nGraph — граф, гнездо\nScenarios, Settings', C.light],
    ['aco.colony', 'Ant — муравей:\nchooseNext, move\nColony — циклы, феромон\nTour, CycleStats, BruteForce', C.cheeseBg],
    ['aco.controller', 'AppController — кнопки,\nтаймер, щелчки\nSettingsForm — проверка\nввода', C.pathBg],
    ['aco.view', 'AntApp — окно\nGraphCanvas — граф\nи муравьи', C.waterBg],
  ];
  cols.forEach(([h, b, fill], i) => {
    const x = 0.6 + i * 3.1;
    card(s, x, 1.9, 2.85, 3.6, fill);
    s.addText(h, { x: x + 0.2, y: 2.05, w: 2.5, h: 0.5, fontFace: HEAD, fontSize: 18, bold: true, color: C.ink, margin: 0, isTextBox: true });
    s.addText(b, { x: x + 0.2, y: 2.65, w: 2.5, h: 2.7, fontFace: BODY, fontSize: 15, color: C.ink, margin: 0, valign: 'top', isTextBox: true });
  });
  s.addText('Модель и колония не зависят от JavaFX; логика окна проверяется тестами без открытия окна. UML — шесть видов диаграмм.', { x: 0.6, y: 5.8, w: 12.1, h: 0.8, fontFace: BODY, fontSize: 16, color: C.muted, margin: 0, isTextBox: true });
  s.addNotes('Инкапсуляция: феромон меняется только методами Path. Полиморфизм: sealed FormResult. Наследование: AntApp — Application, GraphCanvas — Pane.');
}

// ---------- 8. Окно ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Окно программы');
  fit(s, shot('gui_03_running.png'), 0.5, 1.4, 8.2, 5.6);
  bullets(s, [
    'Толщина и цвет линии — феромон.',
    'Зелёный пунктир — лучший маршрут.',
    'Муравьи плавно бегут по путям.',
    'Пуск, Шаг, Цикл, До конца, Сначала.',
    'Граф: шестиугольник, два муравья, случайные и свои города (щелчком).',
    'График длины по циклам и журнал.',
  ], { x: 8.9, y: 1.5, w: 4.0, h: 5.5 }, 15);
  s.addNotes('Анимация: таймер считает прошедшее время, дробную часть отдаёт окну — муравей рисуется между городами.');
}

// ---------- 9. Два муравья ----------
{
  const s = pres.addSlide();
  s.background = { color: C.white };
  title(s, 'Пример «два муравья» в программе', 'Параметры задания; путь сверху 20, снизу 10');
  fit(s, shot('gui_06_two_cycle1.png'), 0.5, 1.8, 6.1, 4.1);
  fit(s, shot('gui_07_two_end.png'), 6.75, 1.8, 6.1, 4.1);
  s.addText('Цикл 1: τ = 0,52 на коротком, 0,04 на длинном', { x: 0.5, y: 6.0, w: 6.1, h: 0.6, align: 'center', fontFace: BODY, fontSize: 16, color: C.ink, margin: 0, isTextBox: true });
  s.addText('Цикл 100: феромон только на коротком', { x: 6.75, y: 6.0, w: 6.1, h: 0.6, align: 'center', fontFace: BODY, fontSize: 16, bold: true, color: BEST, margin: 0, isTextBox: true });
  s.addNotes('Короткий путь находится в 945 запусках из 1000; в остальных первые случайные выборы закрепили длинный путь.');
}

// ---------- 10. Итоги ----------
{
  const s = pres.addSlide();
  s.background = { color: C.dark };
  s.addText('Итоги', { x: 0.6, y: 0.5, w: 12, h: 0.8, fontFace: HEAD, fontSize: 36, bold: true, color: C.white, margin: 0, isTextBox: true });
  const items = [
    ['Пример задания', '0,4 / 0,7 → 0,16 / 0,28 — до цифры'],
    ['Сначала наугад', 'в первом цикле почти все дороги'],
    ['Потом по кратчайшему', 'вся колония на оптимуме перебора'],
    ['ООП, UML, тесты', 'Ant, Node, Path; шесть видов UML; 80 автотестов'],
  ];
  items.forEach(([h, b], i) => {
    const y = 1.6 + i * 1.25;
    badge(s, 0.6, y, 0.7, ANT, String(i + 1), C.white, 20);
    s.addText(h, { x: 1.6, y, w: 10.5, h: 0.45, fontFace: BODY, fontSize: 22, bold: true, color: C.white, margin: 0, isTextBox: true });
    s.addText(b, { x: 1.6, y: y + 0.45, w: 10.5, h: 0.45, fontFace: BODY, fontSize: 17, color: 'D3D1C7', margin: 0, isTextBox: true });
  });
  s.addNotes('Спасибо за внимание.');
}

pres.writeFile({ fileName: OUT }).then(f => console.log('готово:', f));
