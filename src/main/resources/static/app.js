const form = document.querySelector('#search-form');
const input = document.querySelector('#query');
const status = document.querySelector('#status');
const body = document.querySelector('#results-body');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const query = input.value.trim();
  if (!query) return;
  status.textContent = 'Recherche en cours...';
  body.innerHTML = '';
  try {
    const response = await fetch(`/api/reports/search?q=${encodeURIComponent(query)}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const results = await response.json();
    render(results);
  } catch (error) {
    status.textContent = 'Erreur pendant la recherche.';
    body.innerHTML = `<tr><td class="empty" colspan="7">${escapeHtml(error.message)}</td></tr>`;
  }
});

function render(results) {
  status.textContent = `${results.length} résultat(s)`;
  if (!results.length) {
    body.innerHTML = '<tr><td class="empty" colspan="7">Aucun rapport trouvé.</td></tr>';
    return;
  }
  body.innerHTML = results.map((item) => `
    <tr>
      <td><div class="patient-name">${escapeHtml(joinName(item.patientFirstName, item.patientLastName))}</div><div class="muted">Né(e): ${escapeHtml(item.birthdate || '')}</div></td>
      <td>${escapeHtml(item.patientInternalId || '')}</td>
      <td>${escapeHtml(item.ssn || '')}</td>
      <td>${escapeHtml(item.referenceNumber || '')}</td>
      <td>${escapeHtml(item.validationDate || '')}</td>
      <td>${escapeHtml(item.examinationStatus || '')}</td>
      <td>${item.pdfUrl ? `<a class="pdf-link" href="${item.pdfUrl}" target="_blank" rel="noopener">Ouvrir</a>` : '<span class="muted">Indisponible</span>'}</td>
    </tr>
  `).join('');
}

function joinName(firstName, lastName) {
  return [firstName, lastName].filter(Boolean).join(' ') || 'Patient sans nom';
}

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
}
