import os

admin_path = 'src/main/resources/templates/admin.html'
socia_base_path = 'src/main/resources/templates/socia_agenda_base.html'
socia_dest_path = 'src/main/resources/templates/socia_agenda.html'

with open(admin_path, 'r', encoding='utf-8') as f:
    admin_html = f.read()

with open(socia_base_path, 'r', encoding='utf-8') as f:
    socia_html = f.read()

# Extraccion DOM
dom_start = admin_html.find('<div class="modal fade" id="modalKardex"')
dom_end = admin_html.find('<!-- Modal Caja Fuerte Admin -->')
kardex_dom = admin_html[dom_start:dom_end].strip()

# Extraccion JS
js_start = admin_html.find('let visitasKardexTemporal = [];')
js_end = admin_html.find('// ==========================================\n    // LÓGICA RESTANTE')
if js_end == -1:
    js_end = admin_html.find('// ==========================================\r\n    // LÓGICA RESTANTE')

kardex_js = admin_html[js_start:js_end].strip()

# Inyeccion
socia_html = socia_html.replace('<!-- REEMPLAZO-KARDEX-DOM -->', kardex_dom)
socia_html = socia_html.replace('<!-- REEMPLAZO-KARDEX-JS -->', kardex_js)

with open(socia_dest_path, 'w', encoding='utf-8') as f:
    f.write(socia_html)

print('Kardex inyectado en socia_agenda.html mediante python_script con exito.')
