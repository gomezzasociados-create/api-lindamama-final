const fs = require('fs');
const path = require('path');

const adminPath = path.resolve('src/main/resources/templates/admin.html');
const sociaPath = path.resolve('src/main/resources/templates/socia_agenda.html');

let adminHtml = fs.readFileSync(adminPath, 'utf8');
let sociaHtml = fs.readFileSync(sociaPath, 'utf8');

// Extraer DOM admin
const domStart = adminHtml.indexOf('<div class="modal fade" id="modalKardex"');
const domEnd = adminHtml.indexOf('<!-- Modal Caja Fuerte Admin -->');
const domModal = adminHtml.substring(domStart, domEnd).trim();

// Extraer JS admin
const jsStart = adminHtml.indexOf('let visitasKardexTemporal = [];');
const jsEnd = adminHtml.indexOf('// ==========================================\r\n    // LÓGICA RESTANTE');
let jsLimit = jsEnd > -1 ? jsEnd : adminHtml.indexOf('// ==========================================\n    // LÓGICA RESTANTE');
const jsKardex = adminHtml.substring(jsStart, jsLimit).trim();

// Eliminar el DOM viejo de socia y su JS viejo
// Primero restauro sociaHtml original (ya que el script anterior lo arruinó)
// Para restaurar, recupero de git.
