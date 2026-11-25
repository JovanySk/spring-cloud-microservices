/*
 * Bakery ops console. Plain JavaScript, no build step, no external resources.
 * Everything goes through the gateway that serves this page:
 *   GET  /console/api/status          read-only snapshot (Eureka registry + /actuator/health)
 *   GET  /api/catalog/products        catalog-service
 *   GET  /api/inventory/{sku}         inventory-service
 *   POST /api/orders                  order-service (which calls catalog + inventory with Feign)
 */
(function () {
  'use strict';

  /* ------------------------------------------------------------------ i18n */

  var I18N = {
    en: {
      subtitle: 'Ops console',
      bannerChecking: 'Checking services...',
      chaosSummary: 'Chaos test',
      chaos1: '1. Stop inventory-service (Ctrl+C in its terminal, or stop its container).',
      chaos2: '2. Within a few seconds this banner turns amber and its node goes DOWN.',
      chaos3: '3. Place an order: order-service answers a controlled 503 problem+json, never a hang or a 500.',
      chaos4: '4. Start it again: Eureka picks it up and orders work again.',
      mapTitle: 'Architecture',
      mapSub: "Live from Eureka and each instance's /actuator/health",
      legendRoute: 'route',
      legendBus: 'control plane',
      orderTitle: 'Place order',
      addLine: 'Add line',
      submitOrder: 'Place order',
      sending: 'Sending...',
      resultEmpty: 'The response will appear here, with its status and the hops it took.',
      catalogTitle: 'Catalog and stock',
      reload: 'Reload',
      colProduct: 'Product',
      colPrice: 'Price',
      colStock: 'Stock',
      colActions: 'Actions',
      logTitle: 'Recent requests',
      logSub: 'Through the gateway, newest first',
      clear: 'Clear',
      logEmpty: 'No requests yet.',
      checkedAt: 'Checked {time}',
      allUpTitle: 'All systems operational',
      allUpText: '{up} of {total} components UP. Orders flow gateway, order-service, then catalog and inventory.',
      downTitle: '{names} DOWN',
      downTextBusiness: 'The gateway keeps answering. Orders that need it return a controlled 503 problem+json instead of hanging or failing with a 500.',
      downTextInfra: 'Running services keep their cached registry and config, but new instances cannot register or start.',
      unreachableTitle: 'Gateway unreachable',
      unreachableText: 'The status endpoint did not answer. Is api-gateway running on this address?',
      instances: '{n} instance',
      instancesPlural: '{n} instances',
      noInstances: 'not registered',
      you: 'you',
      thisConsole: 'this console',
      subGateway: 'Spring Cloud Gateway',
      subEureka: 'Eureka registry',
      subConfig: 'config-repo/',
      subCatalog: 'products',
      subInventory: 'stock per SKU',
      subOrder: 'orders',
      busLabel: 'REGISTER + CONFIG',
      loading: 'Loading...',
      catalogDown: 'Catalog unavailable ({status}). Is catalog-service UP?',
      stockNA: 'n/a',
      addToOrder: 'Add {sku} to the order',
      removeLine: 'Remove line',
      qty: 'Quantity',
      product: 'Product',
      unknownSku: 'unknown SKU (404 demo)',
      created: 'Order #{id} created',
      total: 'Total',
      conflict: 'Not enough stock',
      notFound: 'Unknown product',
      unavailable: 'Service unavailable',
      badRequest: 'Invalid request',
      unexpected: 'Unexpected response',
      network: 'Gateway unreachable',
      networkDetail: 'The request never got an HTTP answer.',
      hint503: 'order-service caught the failure and answered a controlled 503. No hang, no stack trace, no 500.',
      hint409: 'inventory-service refused the reservation with an atomic UPDATE; nothing was oversold.',
      hint404: 'The catalog is checked before any stock is reserved, so nothing was reserved.',
      emptyOrder: 'Add at least one line with a quantity of 1 or more.',
      path: 'path',
      statusText: { 200: 'OK', 201: 'Created', 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 500: 'Server Error', 503: 'Unavailable', 0: 'No response' }
    },
    es: {
      subtitle: 'Consola de operaciones',
      bannerChecking: 'Revisando servicios...',
      chaosSummary: 'Prueba de caos',
      chaos1: '1. Deten inventory-service (Ctrl+C en su terminal, o deten su contenedor).',
      chaos2: '2. En pocos segundos este aviso se pone ambar y su nodo pasa a DOWN.',
      chaos3: '3. Haz un pedido: order-service responde un 503 controlado en problem+json, nunca se cuelga ni da 500.',
      chaos4: '4. Vuelve a iniciarlo: Eureka lo registra y los pedidos funcionan otra vez.',
      mapTitle: 'Arquitectura',
      mapSub: 'En vivo desde Eureka y el /actuator/health de cada instancia',
      legendRoute: 'ruta',
      legendBus: 'plano de control',
      orderTitle: 'Hacer pedido',
      addLine: 'Agregar linea',
      submitOrder: 'Hacer pedido',
      sending: 'Enviando...',
      resultEmpty: 'Aqui aparece la respuesta, con su estado y los saltos que recorrio.',
      catalogTitle: 'Catalogo y existencias',
      reload: 'Recargar',
      colProduct: 'Producto',
      colPrice: 'Precio',
      colStock: 'Existencias',
      colActions: 'Acciones',
      logTitle: 'Peticiones recientes',
      logSub: 'A traves del gateway, las mas nuevas primero',
      clear: 'Limpiar',
      logEmpty: 'Aun no hay peticiones.',
      checkedAt: 'Revisado {time}',
      allUpTitle: 'Todo en operacion',
      allUpText: '{up} de {total} componentes UP. Los pedidos van del gateway a order-service y de ahi a catalogo e inventario.',
      downTitle: '{names} DOWN',
      downTextBusiness: 'El gateway sigue respondiendo. Los pedidos que lo necesitan devuelven un 503 controlado en problem+json, sin colgarse ni dar 500.',
      downTextInfra: 'Los servicios activos conservan su registro y configuracion en cache, pero no pueden arrancar instancias nuevas.',
      unreachableTitle: 'Gateway sin respuesta',
      unreachableText: 'El endpoint de estado no respondio. Esta api-gateway corriendo en esta direccion?',
      instances: '{n} instancia',
      instancesPlural: '{n} instancias',
      noInstances: 'sin registro',
      you: 'tu',
      thisConsole: 'esta consola',
      subGateway: 'Spring Cloud Gateway',
      subEureka: 'registro Eureka',
      subConfig: 'config-repo/',
      subCatalog: 'productos',
      subInventory: 'stock por SKU',
      subOrder: 'pedidos',
      busLabel: 'REGISTRO + CONFIG',
      loading: 'Cargando...',
      catalogDown: 'Catalogo no disponible ({status}). Esta catalog-service UP?',
      stockNA: 'n/d',
      addToOrder: 'Agregar {sku} al pedido',
      removeLine: 'Quitar linea',
      qty: 'Cantidad',
      product: 'Producto',
      unknownSku: 'SKU inexistente (demo 404)',
      created: 'Pedido #{id} creado',
      total: 'Total',
      conflict: 'Existencias insuficientes',
      notFound: 'Producto desconocido',
      unavailable: 'Servicio no disponible',
      badRequest: 'Peticion invalida',
      unexpected: 'Respuesta inesperada',
      network: 'Gateway sin respuesta',
      networkDetail: 'La peticion no obtuvo respuesta HTTP.',
      hint503: 'order-service atrapo la falla y respondio un 503 controlado. Sin bloqueo, sin stack trace, sin 500.',
      hint409: 'inventory-service rechazo la reserva con un UPDATE atomico; no se vendio de mas.',
      hint404: 'El catalogo se consulta antes de reservar, asi que no se aparto nada.',
      emptyOrder: 'Agrega al menos una linea con cantidad de 1 o mas.',
      path: 'ruta',
      statusText: { 200: 'OK', 201: 'Created', 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 500: 'Server Error', 503: 'Unavailable', 0: 'Sin respuesta' }
    }
  };

  var SEED_SKUS = [
    { sku: 'BRD-001', name: 'Sourdough Loaf' },
    { sku: 'BRD-002', name: 'Whole Wheat Bread' },
    { sku: 'PST-001', name: 'Butter Croissant' },
    { sku: 'PST-002', name: 'Concha' },
    { sku: 'CAK-001', name: 'Tres Leches Cake' }
  ];
  var UNKNOWN_SKU = 'XXX-999';
  var STOCK_FULL = 60; // bar scale; seed stock is small, this keeps bars readable

  var lang = readPref('console.lang', 'en');
  if (!I18N[lang]) lang = 'en';

  function t(key, vars) {
    var s = I18N[lang][key];
    if (s === undefined) s = I18N.en[key];
    if (s === undefined) return key;
    if (vars) Object.keys(vars).forEach(function (k) { s = s.split('{' + k + '}').join(vars[k]); });
    return s;
  }
  function readPref(k, d) { try { return localStorage.getItem(k) || d; } catch (e) { return d; } }
  function writePref(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* private mode */ } }

  /* ------------------------------------------------------------------ state */

  var state = {
    status: null,
    statusError: false,
    products: null,
    catalogStatus: null,
    stock: {},          // sku -> number | 'na'
    log: [],
    lines: [{ sku: 'PST-001', qty: 3 }, { sku: 'CAK-001', qty: 1 }],
    result: null,       // last order result
    path: null,         // { ok: [edgeIds], fail: [edgeIds], nodes: [], failNodes: [] }
    layout: null,
    busy: false
  };

  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var SVGNS = 'http://www.w3.org/2000/svg';

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function money(v) {
    var n = Number(v);
    if (!isFinite(n)) return String(v);
    return '$' + n.toLocaleString(lang === 'es' ? 'es-MX' : 'en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function clock(d) {
    return d.toLocaleTimeString(lang === 'es' ? 'es-MX' : 'en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  /* ------------------------------------------------------------------ map layouts */

  // Two hand-drawn layouts of the same graph: wide for desktop, tall for phones.
  var LAYOUTS = {
    wide: {
      vb: '0 0 950 376',
      nodes: {
        client:    { kind: 'client',  x: 8,   y: 150, w: 120, h: 64 },
        gateway:   { kind: 'gateway', x: 168, y: 140, w: 190, h: 84 },
        catalog:   { kind: 'service', x: 524, y: 14,  w: 196, h: 70 },
        order:     { kind: 'service', x: 524, y: 147, w: 196, h: 70 },
        inventory: { kind: 'service', x: 524, y: 280, w: 196, h: 70 },
        eureka:    { kind: 'infra',   x: 788, y: 54,  w: 156, h: 80 },
        config:    { kind: 'infra',   x: 788, y: 228, w: 156, h: 80 }
      },
      edges: [
        { id: 'e-client-gw',      cls: 'route', d: 'M128 182 H168' },
        { id: 'e-gw-catalog',     cls: 'route', d: 'M358 168 H390 Q400 168 400 158 V59 Q400 49 410 49 H524' },
        { id: 'e-gw-order',       cls: 'route', d: 'M358 182 H524' },
        { id: 'e-gw-inventory',   cls: 'route', d: 'M358 196 H390 Q400 196 400 206 V305 Q400 315 410 315 H524' },
        { id: 'e-order-catalog',  cls: 'feign', d: 'M680 147 V84' },
        { id: 'e-order-inventory', cls: 'feign', d: 'M680 217 V280' },
        { id: 'b-main',    cls: 'bus', d: 'M760 24 V368' },
        { id: 'b-catalog', cls: 'bus', d: 'M720 49 H760' },
        { id: 'b-order',   cls: 'bus', d: 'M720 182 H760' },
        { id: 'b-inv',     cls: 'bus', d: 'M720 315 H760' },
        { id: 'b-gw',      cls: 'bus', d: 'M263 224 V368 H760' },
        { id: 'b-eureka',  cls: 'bus', d: 'M760 94 H788' },
        { id: 'b-config',  cls: 'bus', d: 'M760 268 H788' }
      ],
      labels: [
        { edge: 'e-gw-catalog', x: 414, y: 42, text: '/api/catalog/**', anchor: 'start' },
        { edge: 'e-gw-order', x: 414, y: 175, text: '/api/orders/**', anchor: 'start' },
        { edge: 'e-gw-inventory', x: 414, y: 308, text: '/api/inventory/**', anchor: 'start' },
        { edge: 'e-order-catalog', x: 688, y: 119, text: 'price', anchor: 'start' },
        { edge: 'e-order-inventory', x: 688, y: 252, text: 'reserve', anchor: 'start' }
      ],
      bus: { x: 772, y: 362, anchor: 'start' },
      compact: false
    },
    tall: {
      vb: '0 0 360 498',
      nodes: {
        client:    { kind: 'client',  x: 104, y: 6,   w: 152, h: 52 },
        gateway:   { kind: 'gateway', x: 82,  y: 90,  w: 196, h: 72 },
        catalog:   { kind: 'service', x: 4,   y: 236, w: 102, h: 92 },
        order:     { kind: 'service', x: 129, y: 236, w: 102, h: 92 },
        inventory: { kind: 'service', x: 254, y: 236, w: 102, h: 92 },
        eureka:    { kind: 'infra',   x: 4,   y: 410, w: 170, h: 80 },
        config:    { kind: 'infra',   x: 186, y: 410, w: 170, h: 80 }
      },
      edges: [
        { id: 'e-client-gw',      cls: 'route', d: 'M180 58 V90' },
        { id: 'e-gw-catalog',     cls: 'route', d: 'M150 162 C150 204 55 194 55 236' },
        { id: 'e-gw-order',       cls: 'route', d: 'M180 162 V236' },
        { id: 'e-gw-inventory',   cls: 'route', d: 'M210 162 C210 204 305 194 305 236' },
        { id: 'e-order-catalog',  cls: 'feign', d: 'M129 290 H106' },
        { id: 'e-order-inventory', cls: 'feign', d: 'M231 290 H254' },
        { id: 'b-main',    cls: 'bus', d: 'M12 372 H348' },
        { id: 'b-catalog', cls: 'bus', d: 'M55 328 V372' },
        { id: 'b-order',   cls: 'bus', d: 'M180 328 V372' },
        { id: 'b-inv',     cls: 'bus', d: 'M305 328 V372' },
        { id: 'b-gw',      cls: 'bus', d: 'M82 126 H12 V372' },
        { id: 'b-eureka',  cls: 'bus', d: 'M89 372 V410' },
        { id: 'b-config',  cls: 'bus', d: 'M271 372 V410' }
      ],
      labels: [
        { edge: 'e-gw-order', x: 186, y: 206, text: '/api/orders/**', anchor: 'start' }
      ],
      bus: { x: 348, y: 390, anchor: 'end' },
      compact: true
    }
  };

  var NODE_INFO = {
    client:    { title: 'browser',          short: 'browser',   port: null,    sub: 'thisConsole' },
    gateway:   { title: 'api-gateway',      short: 'api-gateway', port: ':8080', sub: 'subGateway' },
    catalog:   { title: 'catalog-service',  short: 'catalog',   port: ':8081', sub: 'subCatalog',   service: 'catalog-service' },
    order:     { title: 'order-service',    short: 'order',     port: ':8083', sub: 'subOrder',     service: 'order-service' },
    inventory: { title: 'inventory-service', short: 'inventory', port: ':8082', sub: 'subInventory', service: 'inventory-service' },
    eureka:    { title: 'discovery-server', short: 'discovery-server', port: ':8761', sub: 'subEureka', infra: 'discovery-server' },
    config:    { title: 'config-server',    short: 'config-server', port: ':8888', sub: 'subConfig', infra: 'config-server' }
  };

  function el(name, attrs, parent) {
    var n = document.createElementNS(SVGNS, name);
    if (attrs) Object.keys(attrs).forEach(function (k) { n.setAttribute(k, attrs[k]); });
    if (parent) parent.appendChild(n);
    return n;
  }

  function renderMap() {
    var wrap = $('.map-wrap');
    var name = wrap.clientWidth < 620 ? 'tall' : 'wide';
    state.layout = name;
    var L = LAYOUTS[name];
    var svg = $('#map');
    svg.innerHTML = '';
    svg.setAttribute('viewBox', L.vb);

    var defs = el('defs', null, svg);
    var pat = el('pattern', { id: 'down-fill', width: 7, height: 7, patternUnits: 'userSpaceOnUse', patternTransform: 'rotate(45)' }, defs);
    el('rect', { width: 7, height: 7, style: 'fill: var(--surface-2)' }, pat);
    el('line', { x1: 0, y1: 0, x2: 0, y2: 7, style: 'stroke: var(--danger); stroke-opacity: .16; stroke-width: 3' }, pat);

    var gEdges = el('g', { 'class': 'edges' }, svg);
    L.edges.forEach(function (e) {
      el('path', { id: e.id, 'class': 'edge ' + e.cls, d: e.d }, gEdges);
    });
    var bl = el('text', { 'class': 'bus-label', x: L.bus.x, y: L.bus.y, 'text-anchor': L.bus.anchor }, svg);
    bl.textContent = t('busLabel');
    L.labels.forEach(function (lb) {
      var tx = el('text', { 'class': 'edge-label', 'data-edge': lb.edge, x: lb.x, y: lb.y, 'text-anchor': lb.anchor }, svg);
      tx.textContent = lb.text;
    });

    var gNodes = el('g', { 'class': 'nodes' }, svg);
    Object.keys(L.nodes).forEach(function (id) {
      var n = L.nodes[id], info = NODE_INFO[id];
      var g = el('g', { 'class': 'node kind-' + n.kind, id: 'n-' + id, 'data-status': 'UNKNOWN', transform: 'translate(' + n.x + ' ' + n.y + ')' }, gNodes);
      el('rect', { 'class': 'box', width: n.w, height: n.h, rx: 10 }, g);
      var compact = L.compact && n.kind === 'service';
      var title = el('text', { 'class': 'title', x: 12, y: compact ? 22 : 24 }, g);
      title.textContent = compact ? info.short : info.title;

      var sub = el('text', { 'class': 'sub', x: 12, y: compact ? 39 : 42 }, g);
      if (n.kind === 'client') {
        sub.textContent = t('thisConsole');
      } else if (compact) {
        sub.textContent = info.port;
      } else {
        sub.textContent = info.port + ' · ' + t(info.sub);
      }

      if (n.kind === 'client') return;
      var sy = n.h - 15;
      el('circle', { 'class': 'sdot', cx: 16, cy: sy - 3.5, r: 3.5 }, g);
      var st = el('text', { 'class': 'status', x: 25, y: sy }, g);
      st.textContent = '...';
      if (n.kind === 'service') {
        var meta = el('text', { 'class': 'meta', x: compact ? 12 : n.w - 12, y: compact ? sy - 18 : sy, 'text-anchor': compact ? 'start' : 'end' }, g);
        meta.textContent = '';
      }
    });

    el('circle', { 'class': 'packet', id: 'packet', r: 4.5, cx: -20, cy: -20, opacity: 0 }, svg);

    applyStatusToMap();
    applyPath();
  }

  function setNodeStatus(id, status, metaText) {
    var g = document.getElementById('n-' + id);
    if (!g) return;
    g.setAttribute('data-status', status);
    var st = g.querySelector('.status');
    if (st) st.textContent = status === 'UNKNOWN' ? '...' : status;
    var meta = g.querySelector('.meta');
    if (meta && metaText !== undefined) meta.textContent = metaText;
    var old = g.querySelector('.down-x');
    if (old) old.parentNode.removeChild(old);
  }

  function applyStatusToMap() {
    var s = state.status;
    if (!s) {
      Object.keys(NODE_INFO).forEach(function (id) {
        if (id !== 'client') setNodeStatus(id, state.statusError && id === 'gateway' ? 'DOWN' : 'UNKNOWN', '');
      });
      return;
    }
    setNodeStatus('gateway', s.gateway.status);
    s.infrastructure.forEach(function (n) {
      var id = n.name === 'discovery-server' ? 'eureka' : n.name === 'config-server' ? 'config' : null;
      if (id) setNodeStatus(id, n.status);
    });
    s.services.forEach(function (svc) {
      var id = svc.name.replace('-service', '');
      var count = svc.instances.length;
      var meta = count === 0 ? t('noInstances') : t(count === 1 ? 'instances' : 'instancesPlural', { n: count });
      setNodeStatus(id, svc.status === 'UP' ? 'UP' : 'DOWN', meta);
    });
  }

  /* ------------------------------------------------------------------ path highlight + packet */

  function clearPath() {
    document.querySelectorAll('#map .edge').forEach(function (p) { p.classList.remove('active', 'ok', 'fail', 'flash'); });
    document.querySelectorAll('#map .edge-label').forEach(function (p) { p.classList.remove('on', 'fail'); });
    document.querySelectorAll('#map .node').forEach(function (n) { n.classList.remove('on-path', 'fail-node'); });
  }

  function applyPath() {
    clearPath();
    var p = state.path;
    if (!p) return;
    p.ok.forEach(function (id) { mark(id, 'ok'); });
    p.fail.forEach(function (id) { mark(id, 'fail'); });
    p.nodes.forEach(function (id) { var n = document.getElementById('n-' + id); if (n) n.classList.add('on-path'); });
    p.failNodes.forEach(function (id) { var n = document.getElementById('n-' + id); if (n) n.classList.add('fail-node'); });
  }

  function mark(edgeId, cls) {
    var e = document.getElementById(edgeId);
    if (e) { e.classList.remove('active', 'ok', 'fail'); e.classList.add(cls); }
    document.querySelectorAll('#map .edge-label[data-edge="' + edgeId + '"]').forEach(function (l) {
      l.classList.add(cls === 'fail' ? 'fail' : 'on');
    });
  }

  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function travel(edgeId, reverse, failing) {
    return new Promise(function (resolve) {
      var path = document.getElementById(edgeId);
      var dot = document.getElementById('packet');
      if (!path || !dot) { resolve(); return; }
      path.classList.add(failing ? 'fail' : 'active');
      if (reduceMotion) { resolve(); return; }
      var len = path.getTotalLength();
      var dur = Math.max(170, Math.min(340, len * 1.6));
      dot.setAttribute('class', failing ? 'packet fail' : 'packet');
      dot.setAttribute('opacity', 1);
      var start = null;
      function frame(ts) {
        if (start === null) start = ts;
        var k = Math.min(1, (ts - start) / dur);
        var e = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
        var pt = path.getPointAtLength(len * (reverse ? 1 - e : e));
        dot.setAttribute('cx', pt.x);
        dot.setAttribute('cy', pt.y);
        if (k < 1) requestAnimationFrame(frame); else resolve();
      }
      requestAnimationFrame(frame);
    });
  }
  function hidePacket() { var d = document.getElementById('packet'); if (d) d.setAttribute('opacity', 0); }

  function flash(edgeIds) {
    edgeIds.forEach(function (id) {
      var e = document.getElementById(id);
      if (!e || e.classList.contains('ok') || e.classList.contains('fail')) return;
      e.classList.add('flash');
      setTimeout(function () { e.classList.remove('flash'); }, 650);
    });
  }

  /* ------------------------------------------------------------------ HTTP + request log */

  function call(method, path, body) {
    var t0 = performance.now();
    var opts = { method: method, headers: { Accept: 'application/json, application/problem+json' }, cache: 'no-store' };
    if (body !== undefined) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    return fetch(path, opts).then(function (res) {
      return res.text().then(function (txt) {
        var data = null;
        try { data = txt ? JSON.parse(txt) : null; } catch (e) { data = { raw: txt }; }
        return { status: res.status, data: data, type: res.headers.get('content-type') || '' };
      });
    }, function (err) {
      return { status: 0, data: { detail: err.message }, type: '' };
    }).then(function (r) {
      r.ms = Math.round(performance.now() - t0);
      addLog(method, path, r.status, r.ms);
      return r;
    });
  }

  function addLog(method, path, status, ms) {
    state.log.unshift({ at: new Date(), method: method, path: path, status: status, ms: ms, fresh: true });
    if (state.log.length > 40) state.log.length = 40;
    renderLog();
  }

  function renderLog() {
    var ol = $('#log');
    if (!state.log.length) {
      ol.innerHTML = '<li class="log-empty">' + esc(t('logEmpty')) + '</li>';
      return;
    }
    ol.innerHTML = state.log.map(function (r) {
      var cls = 's s' + String(r.status).charAt(0);
      if (r.status === 0) cls = 's s0';
      var html = '<li' + (r.fresh ? ' class="fresh"' : '') + '>' +
        '<span class="t">' + esc(clock(r.at)) + '</span>' +
        '<span class="m">' + esc(r.method) + '</span>' +
        '<span class="p" title="' + esc(r.path) + '">' + esc(r.path) + '</span>' +
        '<span class="' + cls + '">' + (r.status || 'ERR') + '</span>' +
        '<span class="l">' + r.ms + ' ms</span></li>';
      r.fresh = false;
      return html;
    }).join('');
  }

  /* ------------------------------------------------------------------ status polling + banner */

  function pollStatus() {
    return fetch('/console/api/status', { cache: 'no-store', headers: { Accept: 'application/json' } })
      .then(function (res) { if (!res.ok) throw new Error(res.status); return res.json(); })
      .then(function (s) { state.status = s; state.statusError = false; },
            function () { state.status = null; state.statusError = true; })
      .then(function () { applyStatusToMap(); renderBanner(); });
  }

  function downNames() {
    var s = state.status;
    if (!s) return { business: [], infra: [] };
    return {
      business: s.services.filter(function (x) { return x.status !== 'UP'; }).map(function (x) { return x.name; }),
      infra: s.infrastructure.filter(function (x) { return x.status !== 'UP'; }).map(function (x) { return x.name; })
    };
  }

  function renderBanner() {
    var b = $('#banner'), title = $('#banner-title'), text = $('#banner-text');
    var dot = $('#gw-dot');
    title.removeAttribute('data-i18n');
    if (state.statusError) {
      b.dataset.state = 'down';
      dot.className = 'dot down';
      title.textContent = t('unreachableTitle');
      text.textContent = t('unreachableText');
      return;
    }
    var s = state.status;
    if (!s) { b.dataset.state = 'unknown'; title.textContent = t('bannerChecking'); text.textContent = ''; return; }
    dot.className = 'dot up';
    $('#checked-at').textContent = t('checkedAt', { time: clock(new Date(s.checkedAt)) });
    var d = downNames();
    var total = 1 + s.infrastructure.length + s.services.length;
    var down = d.business.concat(d.infra);
    if (!down.length) {
      b.dataset.state = 'ok';
      title.textContent = t('allUpTitle');
      text.textContent = t('allUpText', { up: total, total: total });
    } else {
      b.dataset.state = 'warn';
      title.textContent = t('downTitle', { names: down.join(', ') });
      text.textContent = d.business.length ? t('downTextBusiness') : t('downTextInfra');
    }
  }

  /* ------------------------------------------------------------------ catalog + stock */

  function loadCatalog() {
    var tbody = $('#catalog tbody');
    if (!state.products) tbody.innerHTML = '<tr><td colspan="5" class="table-msg">' + esc(t('loading')) + '</td></tr>';
    flash(['e-client-gw', 'e-gw-catalog']);
    return call('GET', '/api/catalog/products').then(function (r) {
      state.catalogStatus = r.status;
      if (r.status === 200 && Array.isArray(r.data)) {
        state.products = r.data;
      } else {
        state.products = null;
      }
      renderCatalog();
      renderLines();
      if (state.products) return loadStock(state.products.map(function (p) { return p.sku; }));
    });
  }

  function loadStock(skus) {
    flash(['e-client-gw', 'e-gw-inventory']);
    return Promise.all(skus.map(function (sku) {
      return call('GET', '/api/inventory/' + encodeURIComponent(sku)).then(function (r) {
        state.stock[sku] = (r.status === 200 && r.data) ? r.data.quantity : 'na';
      });
    })).then(renderCatalog);
  }

  function renderCatalog() {
    var tbody = $('#catalog tbody');
    if (!state.products) {
      tbody.innerHTML = '<tr><td colspan="5" class="table-msg">' + esc(t('catalogDown', { status: state.catalogStatus || 'ERR' })) + '</td></tr>';
      return;
    }
    tbody.innerHTML = state.products.map(function (p) {
      var q = state.stock[p.sku];
      var stockHtml;
      if (q === undefined) {
        stockHtml = '<div class="stock"><div class="stock-bar"><i style="width:0"></i></div><span class="stock-n">..</span></div>';
      } else if (q === 'na') {
        stockHtml = '<div class="stock"><div class="stock-bar none"><i style="width:0"></i></div><span class="stock-n na">' + esc(t('stockNA')) + '</span></div>';
      } else {
        var cls = q === 0 ? 'none' : q <= 5 ? 'low' : '';
        var w = Math.max(q === 0 ? 0 : 4, Math.min(100, Math.round(q / STOCK_FULL * 100)));
        stockHtml = '<div class="stock"><div class="stock-bar ' + cls + '"><i style="width:' + w + '%"></i></div><span class="stock-n ' + cls + '">' + q + '</span></div>';
      }
      return '<tr>' +
        '<td class="sku">' + esc(p.sku) + '</td>' +
        '<td class="name">' + esc(p.name) + '<span class="sku-sub">' + esc(p.sku) + '</span></td>' +
        '<td class="num">' + esc(money(p.price)) + '</td>' +
        '<td>' + stockHtml + '</td>' +
        '<td class="num"><button type="button" class="icon-btn add-btn" data-add="' + esc(p.sku) + '" aria-label="' + esc(t('addToOrder', { sku: p.sku })) + '" title="' + esc(t('addToOrder', { sku: p.sku })) + '">' + plusIcon() + '</button></td>' +
        '</tr>';
    }).join('');
  }

  function plusIcon() {
    return '<svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true"><path d="M7 2.5v9M2.5 7h9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>';
  }
  function minusIcon() {
    return '<svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true"><path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>';
  }

  /* ------------------------------------------------------------------ order form */

  function skuOptions() {
    var list = state.products ? state.products.map(function (p) { return { sku: p.sku, name: p.name }; }) : SEED_SKUS;
    return list.concat([{ sku: UNKNOWN_SKU, name: t('unknownSku') }]);
  }

  function renderLines() {
    var opts = skuOptions();
    $('#lines').innerHTML = state.lines.map(function (line, i) {
      var options = opts.map(function (o) {
        return '<option value="' + esc(o.sku) + '"' + (o.sku === line.sku ? ' selected' : '') + '>' + esc(o.sku + '  ·  ' + o.name) + '</option>';
      }).join('');
      return '<div class="line">' +
        '<select class="field" data-i="' + i + '" data-k="sku" aria-label="' + esc(t('product')) + '">' + options + '</select>' +
        '<input class="field" type="number" min="1" max="999" inputmode="numeric" data-i="' + i + '" data-k="qty" value="' + esc(line.qty) + '" aria-label="' + esc(t('qty')) + '">' +
        '<button type="button" class="icon-btn" data-remove="' + i + '" aria-label="' + esc(t('removeLine')) + '" title="' + esc(t('removeLine')) + '">' + minusIcon() + '</button>' +
        '</div>';
    }).join('');
  }

  function addSku(sku) {
    var found = state.lines.filter(function (l) { return l.sku === sku; })[0];
    if (found) found.qty = Number(found.qty || 0) + 1; else state.lines.push({ sku: sku, qty: 1 });
    renderLines();
  }

  function submitOrder(ev) {
    ev.preventDefault();
    if (state.busy) return;
    var items = state.lines
      .filter(function (l) { return l.sku && Number(l.qty) >= 1; })
      .map(function (l) { return { sku: l.sku, quantity: Math.floor(Number(l.qty)) }; });
    if (!items.length) {
      renderResult({ local: true, status: 400, data: { detail: t('emptyOrder') }, ms: 0 });
      return;
    }
    state.busy = true;
    var btn = $('#submit-order');
    btn.disabled = true;
    btn.textContent = t('sending');
    state.path = null;
    clearPath();
    markNodes(['client', 'gateway', 'order']);

    var request = call('POST', '/api/orders', { items: items });
    var outbound = travel('e-client-gw', false).then(function () { return travel('e-gw-order', false); });

    Promise.all([request, outbound]).then(function (res) {
      var r = res[0];
      return playReturn(r).then(function () { return r; });
    }).then(function (r) {
      hidePacket();
      applyPath();
      renderResult(r);
      var skus = items.map(function (i) { return i.sku; }).filter(function (s) { return s !== UNKNOWN_SKU; });
      return loadStock(skus);
    }).then(function () {
      state.busy = false;
      btn.disabled = false;
      btn.textContent = t('submitOrder');
    });
  }

  function markNodes(ids) {
    ids.forEach(function (id) { var n = document.getElementById('n-' + id); if (n) n.classList.add('on-path'); });
  }

  // Decide which hop failed, then play the rest of the trip and remember the final path.
  function playReturn(r) {
    var detail = (r.data && (r.data.detail || r.data.message || '')) + '';
    var ok = ['e-client-gw', 'e-gw-order'], fail = [], nodes = ['client', 'gateway', 'order'], failNodes = [];
    var steps = [];
    var downSvc = downNames().business;

    if (r.status === 201) {
      steps = [['e-order-catalog'], ['e-order-catalog', 1], ['e-order-inventory'], ['e-order-inventory', 1]];
      ok.push('e-order-catalog', 'e-order-inventory');
      nodes.push('catalog', 'inventory');
    } else if (r.status === 409) {
      steps = [['e-order-catalog'], ['e-order-catalog', 1], ['e-order-inventory', 0, 1], ['e-order-inventory', 1, 1]];
      ok.push('e-order-catalog'); fail.push('e-order-inventory');
      nodes.push('catalog'); failNodes.push('inventory');
    } else if (r.status === 404) {
      steps = [['e-order-catalog', 0, 1], ['e-order-catalog', 1, 1]];
      fail.push('e-order-catalog'); failNodes.push('catalog');
    } else if (r.status === 503 && (/inventory/i.test(detail) || (!/catalog/i.test(detail) && downSvc.indexOf('inventory-service') >= 0))) {
      steps = [['e-order-catalog'], ['e-order-catalog', 1], ['e-order-inventory', 0, 1]];
      ok.push('e-order-catalog'); fail.push('e-order-inventory');
      nodes.push('catalog'); failNodes.push('inventory');
    } else if (r.status === 503 && /catalog/i.test(detail)) {
      steps = [['e-order-catalog', 0, 1]];
      fail.push('e-order-catalog'); failNodes.push('catalog');
    } else if (r.status === 0) {
      ok = []; fail = ['e-client-gw']; nodes = ['client']; failNodes = ['gateway'];
    } else if (r.status >= 500) {
      ok = ['e-client-gw']; fail = ['e-gw-order']; nodes = ['client', 'gateway']; failNodes = ['order'];
    }
    var back = r.status >= 200 && r.status < 300 ? 0 : 1;
    if (r.status !== 0) steps.push(['e-gw-order', 1, back && r.status >= 500 && fail.indexOf('e-gw-order') >= 0 ? 1 : 0], ['e-client-gw', 1, 0]);

    return steps.reduce(function (p, s) {
      return p.then(function () { return travel(s[0], !!s[1], !!s[2]); });
    }, Promise.resolve()).then(function () {
      state.path = { ok: ok, fail: fail, nodes: nodes, failNodes: failNodes };
    });
  }

  // The hops the request took, as drawn on the map (failed hop in red).
  function hopsHtml() {
    var p = state.path;
    if (!p) return '';
    var names = { client: 'browser', gateway: 'api-gateway', order: 'order-service', catalog: 'catalog-service', inventory: 'inventory-service' };
    var hops = p.nodes.map(function (n) { return '<span class="hop">' + names[n] + '</span>'; })
      .concat(p.failNodes.map(function (n) { return '<span class="hop fail">' + names[n] + '</span>'; }));
    return '<div class="hops"><span class="hops-label">' + esc(t('path')) + '</span>' + hops.join('<span class="arrow" aria-hidden="true">→</span>') + '</div>';
  }

  function renderResult(r) {
    state.result = r;
    var box = $('#result');
    if (!r) { box.innerHTML = '<p class="empty">' + esc(t('resultEmpty')) + '</p>'; return; }
    var d = r.data || {};
    var code = r.status;
    var pill = code ? code + ' ' + (I18N[lang].statusText[code] || '') : 'ERR';
    var foot = '<div class="card-foot"><span>' + esc((r.type || '').split(';')[0] || (r.local ? 'client-side check' : '')) + '</span><span>' + (r.ms || 0) + ' ms</span></div>';

    if (code === 201) {
      var rows = (d.items || []).map(function (l) {
        return '<tr><td><span class="sku">' + esc(l.sku) + '</span>' + esc(l.productName) + ' × ' + l.quantity + '</td><td class="num">' + esc(money(l.lineTotal)) + '</td></tr>';
      }).join('');
      box.innerHTML = '<article class="card ok">' +
        '<div class="card-head"><span class="card-title">' + esc(t('created', { id: d.id })) + '</span><span class="status-pill">' + esc(pill) + '</span></div>' +
        '<div class="card-body"><table class="lines-table"><tbody>' + rows + '</tbody></table>' +
        '<div class="total"><span>' + esc(t('total')) + '</span><strong>' + esc(money(d.total)) + '</strong></div></div>' +
        hopsHtml() + foot + '</article>';
      return;
    }

    var title, tone, hint = '';
    if (code === 409) { title = t('conflict'); tone = 'warn'; hint = t('hint409'); }
    else if (code === 404) { title = t('notFound'); tone = 'warn'; hint = t('hint404'); }
    else if (code === 400) { title = t('badRequest'); tone = 'warn'; }
    else if (code === 503) { title = t('unavailable'); tone = 'err'; hint = t('hint503'); }
    else if (code === 0) { title = t('network'); tone = 'err'; }
    else { title = t('unexpected'); tone = 'err'; }

    var detail = d.detail || d.message || d.error || (code === 0 ? t('networkDetail') : '');
    var kv = '';
    if (!r.local && code !== 0) {
      kv = '<dl class="kv">' +
        (d.type ? '<dt>type</dt><dd>' + esc(d.type) + '</dd>' : '') +
        (d.title ? '<dt>title</dt><dd>' + esc(d.title) + '</dd>' : '') +
        '<dt>status</dt><dd>' + esc(d.status || code) + '</dd>' +
        (d.instance ? '<dt>instance</dt><dd>' + esc(d.instance) + '</dd>' : '') +
        '</dl>';
    }
    box.innerHTML = '<article class="card ' + tone + '">' +
      '<div class="card-head"><span class="card-title">' + esc(title) + '</span><span class="status-pill">' + esc(pill) + '</span></div>' +
      '<div class="card-body"><p class="detail">' + esc(detail) + '</p>' + (hint ? '<p>' + esc(hint) + '</p>' : '') + kv + '</div>' +
      (r.local ? '' : hopsHtml()) + foot + '</article>';
  }

  /* ------------------------------------------------------------------ language */

  function applyLang() {
    document.documentElement.lang = lang;
    document.querySelectorAll('[data-i18n]').forEach(function (n) { n.textContent = t(n.getAttribute('data-i18n')); });
    document.querySelectorAll('.lang button').forEach(function (b) { b.setAttribute('aria-pressed', String(b.dataset.lang === lang)); });
    if (state.busy) $('#submit-order').textContent = t('sending');
    renderMap();
    renderBanner();
    renderCatalog();
    renderLines();
    renderLog();
    if (state.result) renderResult(state.result);
  }

  /* ------------------------------------------------------------------ wiring */

  function init() {
    $('#gw-origin').textContent = location.host || 'localhost:8080';

    document.querySelectorAll('.lang button').forEach(function (b) {
      b.addEventListener('click', function () { lang = b.dataset.lang; writePref('console.lang', lang); applyLang(); });
    });
    $('#order-form').addEventListener('submit', submitOrder);
    $('#add-line').addEventListener('click', function () {
      var used = state.lines.map(function (l) { return l.sku; });
      var next = skuOptions().filter(function (o) { return used.indexOf(o.sku) < 0 && o.sku !== UNKNOWN_SKU; })[0];
      state.lines.push({ sku: next ? next.sku : SEED_SKUS[0].sku, qty: 1 });
      renderLines();
    });
    $('#lines').addEventListener('change', onLineEdit);
    $('#lines').addEventListener('input', onLineEdit);
    $('#lines').addEventListener('click', function (e) {
      var b = e.target.closest('[data-remove]');
      if (!b) return;
      state.lines.splice(Number(b.dataset.remove), 1);
      renderLines();
    });
    $('#catalog').addEventListener('click', function (e) {
      var b = e.target.closest('[data-add]');
      if (b) addSku(b.dataset.add);
    });
    $('#reload-catalog').addEventListener('click', loadCatalog);
    $('#clear-log').addEventListener('click', function () { state.log = []; renderLog(); });

    var lastW = 0;
    window.addEventListener('resize', function () {
      var w = $('.map-wrap').clientWidth;
      var name = w < 620 ? 'tall' : 'wide';
      if (name !== state.layout && Math.abs(w - lastW) > 4) { lastW = w; renderMap(); }
    });

    applyLang();
    pollStatus().then(loadCatalog);
    setInterval(pollStatus, 4000);
  }

  function onLineEdit(e) {
    var i = e.target.dataset.i, k = e.target.dataset.k;
    if (i === undefined) return;
    state.lines[Number(i)][k] = k === 'qty' ? e.target.value : e.target.value;
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
