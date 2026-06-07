// ==========================================
// Nav: transparent at top, solid on scroll
// ==========================================
(function () {
  const nav = document.querySelector('.nav');
  if (!nav) return;
  window.addEventListener('scroll', () => {
    nav.style.background = window.scrollY > 50 ? '#000' : 'transparent';
  });
})();

// ==========================================
// Hero devices: hover to play, pause others
// ==========================================
(function () {
  const titan = document.querySelector('.hero-device--hero');
  const sides = document.querySelectorAll('.hero-device--side');
  if (!titan || !sides.length) return;

  const titanVideo = titan.querySelector('video');
  const allSides = Array.from(sides);

  allSides.forEach(device => {
    const video = device.querySelector('video');
    if (!video) return;

    device.addEventListener('mouseenter', () => {
      if (titanVideo) titanVideo.pause();
      video.play();
    });

    device.addEventListener('mouseleave', () => {
      video.pause();
      if (titanVideo) titanVideo.play();
    });
  });
})();

// ==========================================
// Hero: staggered fade in on load
// ==========================================
(function () {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => document.body.classList.add('page-loaded'));
  });
})();

// ==========================================
// Scroll reveal: fade in sections on scroll
// ==========================================
(function () {
  const sections = document.querySelectorAll('.features-wrap, .features-header, .features-container, .cards-section, .testimonials, .tall-card, .feature-section, .categories, .cat-card');
  sections.forEach(el => el.classList.add('reveal'));

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.1, rootMargin: '0px 0px -50px 0px' });

  sections.forEach(el => observer.observe(el));
})();

// ==========================================
// Lazy loading: load images/videos when visible
// ==========================================
(function () {
  document.querySelectorAll('img').forEach(img => {
    img.setAttribute('loading', 'lazy');
  });
  document.querySelectorAll('video').forEach(video => {
    video.setAttribute('preload', 'none');
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          video.setAttribute('preload', 'auto');
          video.load();
          observer.unobserve(video);
        }
      });
    }, { rootMargin: '200px' });
    observer.observe(video);
  });
})();

// ==========================================
// Testimonials: match feature card widths + infinite carousel
// ==========================================
// ==========================================
// Testimonials: simple scroll carousel
// ==========================================
(function () {
  const track = document.querySelector('.testimonials-track');
  if (!track) return;

  const prevBtn = document.querySelector('.testimonial-prev');
  const nextBtn = document.querySelector('.testimonial-next');

  function getStep() {
    const card = track.querySelector('.testimonial');
    if (!card) return 0;
    const gap = parseFloat(getComputedStyle(track).gap) || 24;
    return card.getBoundingClientRect().width + gap;
  }

  if (nextBtn) nextBtn.addEventListener('click', function () {
    track.scrollBy({ left: getStep(), behavior: 'smooth' });
  });
  if (prevBtn) prevBtn.addEventListener('click', function () {
    track.scrollBy({ left: -getStep(), behavior: 'smooth' });
  });
})();

// ==========================================
// Theme demo: click to cycle slides
// ==========================================
(function () {
  var card = document.getElementById('themeDemoCell');
  if (!card) return;

  var slides = card.querySelectorAll('.home-slide');
  var hint = card.querySelector('.tap-hint');
  if (!slides.length) return;

  var current = 0;
  slides[0].classList.add('active');

  function next() {
    slides[current].classList.remove('active');
    current = (current + 1) % slides.length;
    slides[current].classList.add('active');
    if (hint) hint.classList.add('tap-hint-hidden');
  }

  card.addEventListener('click', next);
})();

// ==========================================
// Gesture demo: click to cycle slides
// ==========================================
(function () {
  var card = document.getElementById('gestureDemoCell');
  if (!card) return;

  var slides = card.querySelectorAll('.home-slide');
  var hint = card.querySelector('.tap-hint');
  if (!slides.length) return;

  var current = 0;

  function next() {
    slides[current].classList.remove('active');
    current = (current + 1) % slides.length;
    slides[current].classList.add('active');
    if (hint) hint.classList.add('tap-hint-hidden');
  }

  card.addEventListener('click', next);
})();

// ==========================================
// Notification demo: click to cycle slides
// ==========================================
(function () {
  var card = document.getElementById('notifDemoCell');
  if (!card) return;

  var slides = card.querySelectorAll('.home-slide');
  var hint = card.querySelector('.tap-hint');
  if (!slides.length) return;

  var current = 0;

  function next() {
    slides[current].classList.remove('active');
    current = (current + 1) % slides.length;
    slides[current].classList.add('active');
    if (hint) hint.classList.add('tap-hint-hidden');
  }

  card.addEventListener('click', next);
})();

// ==========================================
// Search demo: typing animation (hover only)
// ==========================================
(function () {
  var q = document.getElementById('searchQuery');
  var r = document.getElementById('searchResults');
  var l = document.getElementById('searchLabel');
  if (!q || !r || !l) return;

  var card = document.getElementById('searchDemoCell');
  var apps = ['Calculator', 'Calendar', 'Caliber', 'Camera', 'Chrome', 'Claude', 'Clock', 'Contacts'];

  var slides = [
    { label: 'Apps', query: 'Mess', results: ['Messages', 'Messenger'], web: true },
    { label: 'Contacts', query: 'Sarah', results: ['Sarah Connor', 'Sarah Miller'], web: true },
    { label: 'Music', query: 'cat stevens', results: ['Cat Stevens - Wild World', 'Cat Stevens - Father and Son', 'Cat Stevens - Peace Train', 'Cat Stevens - Moonshadow'], web: true },
    { label: 'Files', query: 'ticket', results: ['airline_ticket.pdf', 'ticket_receipt.pdf'], web: true },
    { label: 'Web', query: 'ft.com', results: [], web: true },
    { label: 'Settings', query: 'wifi', results: ['Wi-Fi Settings', 'Wi-Fi Hotspot'], web: true },
    { label: 'Web search', query: 'nauru capital?', results: [], web: true }
  ];

  var current = -1;
  var typing = false;
  var timer = null;

  function render(label, queryText, items) {
    l.textContent = label;
    q.textContent = queryText || '>_';
    r.innerHTML = '';
    items.forEach(function (item) {
      var d = document.createElement('div');
      d.className = 'search-result' + (item.web ? ' search-web' : '');
      d.textContent = item.text;
      r.appendChild(d);
    });
  }

  function showApps() {
    render('Search everything', '', apps.map(function (a) { return { text: a, web: false }; }));
  }

  function buildResults(slide, typed) {
    var items = [];
    slide.results.forEach(function (res) {
      items.push({ text: res, web: false });
    });
    if (slide.web) {
      items.push({ text: 'Search web for "' + typed + '"', web: true });
    }
    return items;
  }

  function runSlide() {
    if (typing) {
      clearTimeout(timer);
      typing = false;
    }
    current = (current + 1) % slides.length;
    var slide = slides[current];
    var chars = slide.query.split('');
    var i = 0;
    typing = true;

    render(slide.label, '', []);

    timer = setTimeout(function typeChar() {
      i++;
      var typed = chars.slice(0, i).join('');
      render(slide.label, typed, buildResults(slide, typed));

      if (i < chars.length) {
        timer = setTimeout(typeChar, 80 + Math.random() * 60);
      } else {
        typing = false;
      }
    }, 300);
  }

  var hint = card ? card.querySelector('.tap-hint') : null;

  // Show apps by default
  showApps();

  card.addEventListener('click', function () {
    runSlide();
    if (hint) hint.classList.add('tap-hint-hidden');
  });
})();
