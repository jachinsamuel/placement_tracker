/**
 * Campus Placement Drive Candidate Tracking & Shortlisting Portal
 * Simple, beginner-friendly JavaScript application controller
 */

// Application State
const state = {
    currentView: 'dashboard',
    students: [],
    companies: [],
    shortlists: [],
    currentShortlist: null,
    currentCandidateTab: 'eligible',
    deptChart: null,
    deleteTarget: { type: null, id: null, name: '' }
};

// ==============================================================================
// 1. Initialization and Navigation
// ==============================================================================

document.addEventListener('DOMContentLoaded', () => {
    initRouter();
    loadDashboard();
});

function initRouter() {
    const hash = window.location.hash.replace('#', '');
    if (['dashboard', 'students', 'companies', 'shortlist', 'reports', 'settings'].includes(hash)) {
        navigate(hash);
    } else {
        navigate('dashboard');
    }

    window.addEventListener('hashchange', () => {
        const target = window.location.hash.replace('#', '') || 'dashboard';
        navigate(target, false);
    });
}

function navigate(viewName, updateHash = true) {
    state.currentView = viewName;
    if (updateHash) {
        window.location.hash = viewName;
    }

    // Hide all view sections
    document.querySelectorAll('.app-view').forEach(el => el.classList.add('d-none'));

    // Show the selected view section
    const target = document.getElementById(`view-${viewName}`);
    if (target) {
        target.classList.remove('d-none');
    }

    // Update active state in sidebar
    document.querySelectorAll('.sidebar-menu a').forEach(a => {
        a.classList.remove('active');
        if (a.getAttribute('href') === `#${viewName}`) {
            a.classList.add('active');
        }
    });

    // Update topbar titles
    const headings = {
        dashboard: { title: 'Dashboard', sub: 'Placement overview, drive analytics, and candidate metrics' },
        students: { title: 'Student Management', sub: 'Academic records, backlogs status, and technical skillsets' },
        companies: { title: 'Company Management', sub: 'Visiting recruitment partners and configured eligibility rules' },
        shortlist: { title: 'Generate Candidate Shortlist', sub: 'Evaluate student pool, rank eligible candidates, and export results' },
        reports: { title: 'Shortlist Archives & Reports', sub: 'Historical drive results and reproducible audit snapshots' },
        settings: { title: 'Algorithm & Rules Info', sub: 'Ranking score formula, weight breakdown, and project details' }
    };

    if (headings[viewName]) {
        document.getElementById('pageHeading').textContent = headings[viewName].title;
        document.getElementById('pageSubheading').textContent = headings[viewName].sub;
    }

    // Refresh view data
    if (viewName === 'dashboard') loadDashboard();
    if (viewName === 'students') loadStudents();
    if (viewName === 'companies') loadCompanies();
    if (viewName === 'shortlist') loadShortlistPage();
    if (viewName === 'reports') loadReports();
}

function refreshCurrentView() {
    navigate(state.currentView, false);
    showToast('Data refreshed successfully', 'info');
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('show');
}

// ==============================================================================
// 2. Dashboard Controller
// ==============================================================================

async function loadDashboard() {
    try {
        const [statsRes, deptRes, compRes, shortlistsRes] = await Promise.all([
            fetch('/api/dashboard/stats'),
            fetch('/api/dashboard/department-stats'),
            fetch('/api/dashboard/company-stats'),
            fetch('/api/shortlists')
        ]);

        if (statsRes.ok) {
            const stats = await statsRes.json();
            document.getElementById('statTotalStudents').textContent = stats.totalStudents || 0;
            document.getElementById('statTotalCompanies').textContent = stats.totalCompanies || 0;
            document.getElementById('statLatestEligible').textContent = stats.latestEligibleCount || 0;
            document.getElementById('statTotalShortlists').textContent = stats.totalShortlists || 0;
        }

        if (deptRes.ok) {
            const deptData = await deptRes.json();
            renderDepartmentChart(deptData);
        }

        if (compRes.ok) {
            const compData = await compRes.json();
            renderCompanyStatsTable(compData);
        }

        if (shortlistsRes.ok) {
            const shortlists = await shortlistsRes.json();
            renderRecentShortlistsTable(shortlists.slice(0, 5));
        }
    } catch (err) {
        console.error('Error loading dashboard data:', err);
    }
}

function renderDepartmentChart(deptData) {
    const canvas = document.getElementById('deptChart');
    if (!canvas) return;

    if (state.deptChart) {
        state.deptChart.destroy();
    }

    const labels = deptData.map(d => d.department);
    const counts = deptData.map(d => d.count);

    if (labels.length === 0) {
        labels.push('No Students');
        counts.push(0);
    }

    const ctx = canvas.getContext('2d');
    state.deptChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: counts,
                backgroundColor: [
                    '#0071e3', '#34c759', '#ff9500', '#5856d6', '#af52de', '#30b0c7', '#8e8e93'
                ],
                borderWidth: 3,
                borderColor: '#ffffff',
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { 
                        boxWidth: 10, 
                        boxHeight: 10,
                        usePointStyle: true,
                        pointStyle: 'circle',
                        font: { family: '-apple-system, SF Pro Text, Inter, sans-serif', size: 11, weight: '500' },
                        color: '#48484a',
                        padding: 16
                    }
                }
            },
            cutout: '72%'
        }
    });
}

function renderCompanyStatsTable(companies) {
    const tbody = document.getElementById('companyStatsTableBody');
    if (!tbody) return;

    if (companies.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">No visiting companies registered yet.</td></tr>';
        return;
    }

    tbody.innerHTML = companies.map(c => `
        <tr>
            <td class="fw-semibold" style="color: var(--apple-label);">${escapeHtml(c.name)}</td>
            <td><span class="badge-skill">${escapeHtml(c.jobRole)}</span></td>
            <td><strong style="color: var(--apple-blue); font-variant-numeric: tabular-nums;">${c.minCgpa.toFixed(1)}</strong> CGPA</td>
            <td>
                <span class="badge-eligible">
                    ${c.eligibleCount} Candidates Eligible
                </span>
            </td>
        </tr>
    `).join('');
}

function renderRecentShortlistsTable(shortlists) {
    const tbody = document.getElementById('recentShortlistsTableBody');
    if (!tbody) return;

    if (shortlists.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No shortlists generated yet.</td></tr>';
        return;
    }

    tbody.innerHTML = shortlists.map(s => {
        const dateStr = s.generatedAt ? s.generatedAt.replace('T', ' ').substring(0, 16) : '-';
        return `
            <tr>
                <td class="text-muted small" style="font-variant-numeric: tabular-nums;">${dateStr}</td>
                <td class="fw-semibold">${escapeHtml(s.companyName)}</td>
                <td><span class="badge-skill">${escapeHtml(s.jobRole)}</span></td>
                <td>${s.totalEvaluated}</td>
                <td><span class="badge-eligible">${s.eligibleCount}</span></td>
                <td><span class="badge-ineligible">${s.ineligibleCount}</span></td>
                <td>
                    <button class="btn btn-sm btn-light border py-1 px-3" onclick="viewHistoryDetails(${s.id})">
                        <i class="bi bi-eye text-primary"></i> View
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// ==============================================================================
// 3. Students Controller
// ==============================================================================

async function loadStudents() {
    try {
        const response = await fetch('/api/students');
        if (response.ok) {
            state.students = await response.json();
            renderStudentsTable(state.students);
        }
    } catch (err) {
        showToast('Failed to load students: ' + err.message, 'danger');
    }
}

function renderStudentsTable(students) {
    const tbody = document.getElementById('studentsTableBody');
    const countLabel = document.getElementById('studentsCountLabel');
    if (!tbody) return;

    countLabel.textContent = `Showing ${students.length} students`;

    if (students.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8">
                    <div class="empty-state">
                        <i class="bi bi-people"></i>
                        <h5>No Student Records Found</h5>
                        <p>Add a new student manually or import students using a CSV file.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = students.map(s => {
        const skillsBadges = (s.skills || []).map(sk => `<span class="badge-skill">${escapeHtml(sk.name)}</span>`).join('') || '<span class="text-muted small">None</span>';
        const backlogBadge = s.activeBacklogs > 0
            ? `<span class="badge-ineligible">${s.activeBacklogs} Backlogs</span>`
            : `<span class="badge-eligible">Clear (0)</span>`;

        return `
            <tr>
                <td><span style="font-family: SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.8rem; background: rgba(0,0,0,0.04); padding: 3px 6px; border-radius: 6px; color: #3a3a3c;">${escapeHtml(s.studentId)}</span></td>
                <td>
                    <div class="fw-semibold" style="color: var(--apple-label);">${escapeHtml(s.fullName)}</div>
                    <div class="text-muted small" style="font-size: 0.75rem;">${escapeHtml(s.email)}</div>
                </td>
                <td><span class="badge-skill fw-semibold">${escapeHtml(s.department)}</span></td>
                <td><strong style="color: var(--apple-blue); font-variant-numeric: tabular-nums;">${s.cgpa.toFixed(2)}</strong></td>
                <td>${backlogBadge}</td>
                <td style="max-width: 250px;">${skillsBadges}</td>
                <td style="font-variant-numeric: tabular-nums;">${s.graduationYear}</td>
                <td class="text-end">
                    <div class="d-inline-flex gap-1">
                        <button class="btn btn-sm btn-light border py-1 px-2" onclick="editStudent(${s.id})" title="Edit">
                            <i class="bi bi-pencil" style="font-size: 0.85rem;"></i>
                        </button>
                        <button class="btn btn-sm btn-light border text-danger py-1 px-2" onclick="promptDelete('student', ${s.id}, '${escapeHtml(s.fullName)}')" title="Delete">
                            <i class="bi bi-trash" style="font-size: 0.85rem;"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function handleStudentSearch() {
    const query = document.getElementById('studentSearchInput').value.toLowerCase().trim();
    const dept = document.getElementById('studentDeptFilter').value;
    filterAndRenderStudents(query, dept);
}

function handleStudentDeptFilter() {
    const query = document.getElementById('studentSearchInput').value.toLowerCase().trim();
    const dept = document.getElementById('studentDeptFilter').value;
    filterAndRenderStudents(query, dept);
}

function filterAndRenderStudents(query, dept) {
    const filtered = state.students.filter(s => {
        const matchesQuery = !query ||
            s.fullName.toLowerCase().includes(query) ||
            s.studentId.toLowerCase().includes(query) ||
            s.email.toLowerCase().includes(query);
        const matchesDept = dept === 'ALL' || s.department.toUpperCase() === dept.toUpperCase();
        return matchesQuery && matchesDept;
    });
    renderStudentsTable(filtered);
}

function openAddStudentModal() {
    document.getElementById('studentForm').reset();
    document.getElementById('studentDbId').value = '';
    document.getElementById('studentModalTitle').textContent = 'Add New Student';
    new bootstrap.Modal(document.getElementById('studentModal')).show();
}

function editStudent(id) {
    const s = state.students.find(item => item.id === id);
    if (!s) return;

    document.getElementById('studentDbId').value = s.id;
    document.getElementById('stuStudentId').value = s.studentId;
    document.getElementById('stuFullName').value = s.fullName;
    document.getElementById('stuDepartment').value = s.department;
    document.getElementById('stuGradYear').value = s.graduationYear;
    document.getElementById('stuCgpa').value = s.cgpa;
    document.getElementById('stuBacklogs').value = s.activeBacklogs;
    document.getElementById('stuEmail').value = s.email;
    document.getElementById('stuCertScore').value = s.certificationsScore || 0;

    const skillNames = (s.skills || []).map(sk => sk.name).join(', ');
    document.getElementById('stuSkills').value = skillNames;

    document.getElementById('studentModalTitle').textContent = 'Edit Student Record';
    new bootstrap.Modal(document.getElementById('studentModal')).show();
}

async function saveStudent(e) {
    e.preventDefault();
    const dbId = document.getElementById('studentDbId').value;
    const isEdit = !!dbId;

    const payload = {
        studentId: document.getElementById('stuStudentId').value.trim(),
        fullName: document.getElementById('stuFullName').value.trim(),
        department: document.getElementById('stuDepartment').value.trim(),
        graduationYear: parseInt(document.getElementById('stuGradYear').value),
        cgpa: parseFloat(document.getElementById('stuCgpa').value),
        activeBacklogs: parseInt(document.getElementById('stuBacklogs').value),
        email: document.getElementById('stuEmail').value.trim(),
        certificationsScore: parseFloat(document.getElementById('stuCertScore').value) || 0.0,
        skillNames: document.getElementById('stuSkills').value.trim()
    };

    const url = isEdit ? `/api/students/${dbId}` : '/api/students';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('studentModal')).hide();
            showToast(`Student ${isEdit ? 'updated' : 'created'} successfully!`, 'success');
            await loadStudents();
        } else {
            const err = await res.json();
            showToast(err.message || 'Validation error while saving student', 'danger');
        }
    } catch (err) {
        showToast('Server error: ' + err.message, 'danger');
    }
}

function openImportModal() {
    document.getElementById('importForm').reset();
    new bootstrap.Modal(document.getElementById('importModal')).show();
}

async function uploadStudentCsv(e) {
    e.preventDefault();
    const fileInput = document.getElementById('csvFileInput');
    if (!fileInput.files || fileInput.files.length === 0) return;

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    const btn = document.getElementById('btnUploadCsv');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Importing...';

    try {
        const res = await fetch('/api/students/import-csv', {
            method: 'POST',
            body: formData
        });

        const data = await res.json();
        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('importModal')).hide();
            showToast(`Successfully imported ${data.importedCount} student records!`, 'success');
            await loadStudents();
        } else {
            showToast(data.message || 'Failed to import CSV', 'danger');
        }
    } catch (err) {
        showToast('Import error: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-upload me-1"></i> Start Import';
    }
}

// ==============================================================================
// 4. Companies Controller
// ==============================================================================

async function loadCompanies() {
    try {
        const response = await fetch('/api/companies');
        if (response.ok) {
            state.companies = await response.json();
            await renderCompaniesTable(state.companies);
        }
    } catch (err) {
        showToast('Failed to load companies: ' + err.message, 'danger');
    }
}

async function renderCompaniesTable(companies) {
    const tbody = document.getElementById('companiesTableBody');
    if (!tbody) return;

    if (companies.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="12">
                    <div class="empty-state">
                        <i class="bi bi-building"></i>
                        <h5>No Visiting Companies Registered</h5>
                        <p>Configure recruitment criteria for companies visiting your campus.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    const rows = await Promise.all(companies.map(async c => {
        let eligibleCount = 0;
        try {
            const countRes = await fetch(`/api/companies/${c.id}/eligible-count`);
            if (countRes.ok) {
                const countData = await countRes.json();
                eligibleCount = countData.eligibleCount;
            }
        } catch (ignored) {}

        const skillsBadges = (c.requiredSkills || []).map(sk => `<span class="badge-skill">${escapeHtml(sk.name)}</span>`).join('') || '<span class="text-muted small">None</span>';
        const modeBadge = c.skillMatchingMode === 'ALL'
            ? `<span style="background: rgba(0, 113, 227, 0.1); color: #0071e3; font-weight: 600; padding: 3px 8px; border-radius: 6px; font-size: 0.72rem;">ALL</span>`
            : `<span style="background: rgba(88, 86, 214, 0.1); color: #5856d6; font-weight: 600; padding: 3px 8px; border-radius: 6px; font-size: 0.72rem;">ANY</span>`;

        const activeSwitch = `
            <div class="form-check form-switch d-inline-block">
                <input class="form-check-input" type="checkbox" role="switch" ${c.active ? 'checked' : ''} onchange="toggleCompanyActive(${c.id})">
            </div>
        `;

        return `
            <tr>
                <td><span style="font-family: SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.8rem; background: rgba(0,0,0,0.04); padding: 3px 6px; border-radius: 6px; color: #3a3a3c;">${escapeHtml(c.companyCode || '-')}</span></td>
                <td class="fw-semibold" style="color: var(--apple-label);">${escapeHtml(c.name)}</td>
                <td><span class="badge-skill">${escapeHtml(c.jobRole)}</span></td>
                <td><strong style="color: var(--apple-blue); font-variant-numeric: tabular-nums;">${c.minCgpa.toFixed(1)}</strong></td>
                <td>${c.maxBacklogs}</td>
                <td>${modeBadge}</td>
                <td style="max-width: 200px;">${skillsBadges}</td>
                <td><span class="text-muted small">${escapeHtml(c.eligibleDepartments || 'All')}</span></td>
                <td style="font-variant-numeric: tabular-nums;">${c.eligibleGraduationYear || 'Any'}</td>
                <td>
                    <span class="badge-eligible">
                        ${eligibleCount} Eligible
                    </span>
                </td>
                <td>${activeSwitch}</td>
                <td class="text-end">
                    <div class="d-inline-flex gap-1">
                        <button class="btn btn-sm btn-light border py-1 px-2" onclick="editCompany(${c.id})" title="Edit">
                            <i class="bi bi-pencil" style="font-size: 0.85rem;"></i>
                        </button>
                        <button class="btn btn-sm btn-light border text-danger py-1 px-2" onclick="promptDelete('company', ${c.id}, '${escapeHtml(c.name)}')" title="Delete">
                            <i class="bi bi-trash" style="font-size: 0.85rem;"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }));

    tbody.innerHTML = rows.join('');
}

function openAddCompanyModal() {
    document.getElementById('companyForm').reset();
    document.getElementById('companyDbId').value = '';
    document.getElementById('compCode').value = 'CMP-' + Math.floor(100 + Math.random() * 900);
    document.getElementById('companyModalTitle').textContent = 'Register Visiting Company';
    new bootstrap.Modal(document.getElementById('companyModal')).show();
}

function editCompany(id) {
    const c = state.companies.find(item => item.id === id);
    if (!c) return;

    document.getElementById('companyDbId').value = c.id;
    document.getElementById('compCode').value = c.companyCode || '';
    document.getElementById('compName').value = c.name;
    document.getElementById('compRole').value = c.jobRole;
    document.getElementById('compMinCgpa').value = c.minCgpa;
    document.getElementById('compMaxBacklogs').value = c.maxBacklogs;
    document.getElementById('compSkillMode').value = c.skillMatchingMode || 'ALL';
    document.getElementById('compGradYear').value = c.eligibleGraduationYear || '';
    document.getElementById('compDepts').value = c.eligibleDepartments || '';

    const reqSkills = (c.requiredSkills || []).map(sk => sk.name).join(', ');
    document.getElementById('compSkills').value = reqSkills;

    document.getElementById('companyModalTitle').textContent = 'Edit Company Criteria';
    new bootstrap.Modal(document.getElementById('companyModal')).show();
}

async function saveCompany(e) {
    e.preventDefault();
    const dbId = document.getElementById('companyDbId').value;
    const isEdit = !!dbId;

    const gradYearVal = document.getElementById('compGradYear').value.trim();

    const payload = {
        companyCode: document.getElementById('compCode').value.trim(),
        name: document.getElementById('compName').value.trim(),
        jobRole: document.getElementById('compRole').value.trim(),
        minCgpa: parseFloat(document.getElementById('compMinCgpa').value),
        maxBacklogs: parseInt(document.getElementById('compMaxBacklogs').value),
        skillMatchingMode: document.getElementById('compSkillMode').value,
        eligibleGraduationYear: gradYearVal ? parseInt(gradYearVal) : null,
        eligibleDepartments: document.getElementById('compDepts').value.trim() || null,
        requiredSkillNames: document.getElementById('compSkills').value.trim(),
        active: true
    };

    const url = isEdit ? `/api/companies/${dbId}` : '/api/companies';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('companyModal')).hide();
            showToast(`Company criteria ${isEdit ? 'updated' : 'created'} successfully!`, 'success');
            await loadCompanies();
        } else {
            const err = await res.json();
            showToast(err.message || 'Validation error while saving company criteria', 'danger');
        }
    } catch (err) {
        showToast('Server error: ' + err.message, 'danger');
    }
}

async function toggleCompanyActive(id) {
    try {
        const res = await fetch(`/api/companies/${id}/toggle-active`, { method: 'PUT' });
        if (res.ok) {
            showToast('Company status updated', 'info');
            await loadCompanies();
        }
    } catch (err) {
        showToast('Failed to update company status', 'danger');
    }
}

// ==============================================================================
// 5. Generate Shortlist Controller
// ==============================================================================

async function loadShortlistPage() {
    try {
        const response = await fetch('/api/companies?activeOnly=true');
        if (response.ok) {
            state.companies = await response.json();
            populateShortlistDropdown(state.companies);
        }
    } catch (err) {
        console.error('Error loading companies for shortlist:', err);
    }
}

function populateShortlistDropdown(companies) {
    const select = document.getElementById('shortlistCompanySelect');
    if (!select) return;

    select.innerHTML = '<option value="">-- Choose a visiting company --</option>' +
        companies.map(c => `
            <option value="${c.id}">${escapeHtml(c.name)} - ${escapeHtml(c.jobRole)}</option>
        `).join('');

    onShortlistCompanySelected();
}

function onShortlistCompanySelected() {
    const select = document.getElementById('shortlistCompanySelect');
    const btnRun = document.getElementById('btnRunShortlist');
    const pillsContainer = document.getElementById('criteriaPills');
    const companyId = select.value;

    if (!companyId) {
        btnRun.disabled = true;
        pillsContainer.innerHTML = '<span class="text-muted small fst-italic">Select a company from the dropdown to load rules.</span>';
        return;
    }

    const company = state.companies.find(c => c.id == companyId);
    if (!company) return;

    btnRun.disabled = false;

    const skills = (company.requiredSkills || []).map(s => s.name).join(', ') || 'None';
    const modeBadge = company.skillMatchingMode === 'ALL'
        ? '<span class="badge bg-primary">MATCH ALL</span>'
        : '<span class="badge bg-info text-dark">MATCH ANY</span>';

    pillsContainer.innerHTML = `
        <div class="criteria-pill">
            <small>Min CGPA</small>
            <span>${company.minCgpa}</span>
        </div>
        <div class="criteria-pill">
            <small>Max Backlogs</small>
            <span>${company.maxBacklogs}</span>
        </div>
        <div class="criteria-pill">
            <small>Required Skills (${modeBadge})</small>
            <span>${escapeHtml(skills)}</span>
        </div>
        <div class="criteria-pill">
            <small>Allowed Depts</small>
            <span>${escapeHtml(company.eligibleDepartments || 'All Departments')}</span>
        </div>
        <div class="criteria-pill">
            <small>Eligible Batch</small>
            <span>${company.eligibleGraduationYear || 'Any Year'}</span>
        </div>
    `;
}

function validateWeights() {
    const wCgpa = parseFloat(document.getElementById('weightCgpa').value) || 0;
    const wSkills = parseFloat(document.getElementById('weightSkills').value) || 0;
    const wCerts = parseFloat(document.getElementById('weightCerts').value) || 0;

    const total = wCgpa + wSkills + wCerts;
    const badge = document.getElementById('weightTotalBadge');

    badge.textContent = `Total: ${total.toFixed(0)}%`;
    if (Math.abs(total - 100) < 0.001) {
        badge.style.backgroundColor = 'var(--apple-green)';
        badge.style.color = '#ffffff';
        return true;
    } else {
        badge.style.backgroundColor = 'var(--apple-red)';
        badge.style.color = '#ffffff';
        return false;
    }
}

async function generateShortlist() {
    const select = document.getElementById('shortlistCompanySelect');
    const companyId = select.value;
    if (!companyId) return;

    if (!validateWeights()) {
        showToast('Ranking weights must add up to exactly 100%', 'danger');
        return;
    }

    const payload = {
        cgpaWeight: parseFloat(document.getElementById('weightCgpa').value),
        skillsWeight: parseFloat(document.getElementById('weightSkills').value),
        certsWeight: parseFloat(document.getElementById('weightCerts').value)
    };

    const btn = document.getElementById('btnRunShortlist');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Evaluating & Ranking Candidates...';

    try {
        const response = await fetch(`/api/shortlists/generate/${companyId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            state.currentShortlist = await response.json();
            renderShortlistResults(state.currentShortlist);
            showToast('Shortlisting evaluation and ranking complete!', 'success');
        } else {
            const err = await response.json();
            showToast(err.message || 'Error generating shortlist', 'danger');
        }
    } catch (err) {
        showToast('Execution error: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i> Run Shortlisting & Ranking Engine';
    }
}

function renderShortlistResults(shortlist) {
    document.getElementById('shortlistResultsContainer').classList.remove('d-none');

    document.getElementById('resTotalEvaluated').textContent = shortlist.totalEvaluated;
    document.getElementById('resEligibleCount').textContent = shortlist.eligibleCount;
    document.getElementById('resIneligibleCount').textContent = shortlist.ineligibleCount;

    document.getElementById('eligibleTabBadge').textContent = shortlist.eligibleCount;
    document.getElementById('ineligibleTabBadge').textContent = shortlist.ineligibleCount;

    // Reset filter inputs
    document.getElementById('shortlistFilterInput').value = '';
    document.getElementById('shortlistDeptFilter').value = 'ALL';

    filterShortlistCandidates();
}

function switchCandidateTab(tabName) {
    state.currentCandidateTab = tabName;

    const btnEligible = document.getElementById('tab-eligible-btn');
    const btnIneligible = document.getElementById('tab-ineligible-btn');
    const viewEligible = document.getElementById('eligibleCandidatesView');
    const viewIneligible = document.getElementById('ineligibleCandidatesView');

    if (tabName === 'eligible') {
        btnEligible.classList.add('active');
        btnIneligible.classList.remove('active');
        viewEligible.classList.remove('d-none');
        viewIneligible.classList.add('d-none');
    } else {
        btnIneligible.classList.add('active');
        btnEligible.classList.remove('active');
        viewIneligible.classList.remove('d-none');
        viewIneligible.classList.add('d-none');
    }
}

function filterShortlistCandidates() {
    if (!state.currentShortlist) return;

    const query = document.getElementById('shortlistFilterInput').value.toLowerCase().trim();
    const dept = document.getElementById('shortlistDeptFilter').value;

    const candidates = state.currentShortlist.candidates || [];

    const filterFn = c => {
        const matchesQuery = !query ||
            c.studentName.toLowerCase().includes(query) ||
            c.studentId.toLowerCase().includes(query);
        const matchesDept = dept === 'ALL' || c.department.toUpperCase() === dept.toUpperCase();
        return matchesQuery && matchesDept;
    };

    const filteredEligible = candidates.filter(c => c.eligible && filterFn(c));
    const filteredIneligible = candidates.filter(c => !c.eligible && filterFn(c));

    renderEligibleTable(filteredEligible);
    renderIneligibleTable(filteredIneligible);
}

function renderEligibleTable(candidates) {
    const tbody = document.getElementById('eligibleTableBody');
    if (!tbody) return;

    if (candidates.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9">
                    <div class="empty-state py-4">
                        <i class="bi bi-award fs-2"></i>
                        <h6>No Eligible Candidates Match the Criteria</h6>
                        <p class="small">Try relaxing company eligibility thresholds or adjusting filter parameters.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = candidates.map(c => {
        let rankBadgeClass = '';
        if (c.rankNumber === 1) rankBadgeClass = 'top-1';
        else if (c.rankNumber === 2) rankBadgeClass = 'top-2';
        else if (c.rankNumber === 3) rankBadgeClass = 'top-3';

        return `
            <tr>
                <td><span class="badge-rank ${rankBadgeClass}">#${c.rankNumber}</span></td>
                <td><span style="font-family: SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.8rem; background: rgba(0,0,0,0.04); padding: 3px 6px; border-radius: 6px; color: #3a3a3c;">${escapeHtml(c.studentId)}</span></td>
                <td class="fw-semibold" style="color: var(--apple-label);">${escapeHtml(c.studentName)}</td>
                <td><span class="badge-skill fw-semibold">${escapeHtml(c.department)}</span></td>
                <td><strong style="color: var(--apple-blue); font-variant-numeric: tabular-nums;">${c.cgpa.toFixed(2)}</strong></td>
                <td><span class="badge-eligible">${c.activeBacklogs} Backlogs</span></td>
                <td><span class="badge-skill">${escapeHtml(c.matchedSkillsSummary || 'None')}</span></td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <strong style="color: var(--apple-green-text); font-variant-numeric: tabular-nums; font-size: 0.95rem;">${c.rankingScore.toFixed(2)}</strong>
                        <div class="progress flex-grow-1" style="height: 6px; width: 64px; background: rgba(0,0,0,0.06); border-radius: 9999px;">
                            <div class="progress-bar" style="width: ${c.rankingScore}%; background-color: var(--apple-green); border-radius: 9999px;"></div>
                        </div>
                    </div>
                </td>
                <td><span class="badge-eligible">Eligible</span></td>
            </tr>
        `;
    }).join('');
}

function renderIneligibleTable(candidates) {
    const tbody = document.getElementById('ineligibleTableBody');
    if (!tbody) return;

    if (candidates.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8">
                    <div class="empty-state py-4">
                        <i class="bi bi-check-circle text-success fs-2"></i>
                        <h6>No Ineligible Candidates</h6>
                        <p class="small">All evaluated students met the eligibility criteria!</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = candidates.map(c => {
        return `
            <tr>
                <td><span style="font-family: SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 0.8rem; background: rgba(0,0,0,0.04); padding: 3px 6px; border-radius: 6px; color: #3a3a3c;">${escapeHtml(c.studentId)}</span></td>
                <td class="fw-semibold" style="color: var(--apple-label);">${escapeHtml(c.studentName)}</td>
                <td><span class="badge-skill fw-semibold">${escapeHtml(c.department)}</span></td>
                <td><span style="font-variant-numeric: tabular-nums;">${c.cgpa.toFixed(2)}</span></td>
                <td><span class="badge-ineligible">${c.activeBacklogs} Backlogs</span></td>
                <td style="max-width: 180px;"><span class="text-muted small">${escapeHtml(c.skillsSummary || 'None')}</span></td>
                <td>
                    <span class="text-danger small fw-medium" style="font-size: 0.82rem;">
                        <i class="bi bi-x-circle me-1"></i> ${escapeHtml(c.rejectionReasons || 'Criteria not met')}
                    </span>
                </td>
                <td><span class="badge-ineligible">Ineligible</span></td>
            </tr>
        `;
    }).join('');
}

function exportShortlist(format) {
    if (!state.currentShortlist || !state.currentShortlist.id) {
        showToast('Please generate a shortlist first', 'warning');
        return;
    }
    const id = state.currentShortlist.id;
    window.location.href = `/api/shortlists/${id}/export/${format}`;
}

// ==============================================================================
// 6. Reports & Shortlist Archives Controller
// ==============================================================================

async function loadReports() {
    try {
        const response = await fetch('/api/shortlists');
        if (response.ok) {
            state.shortlists = await response.json();
            renderReportsTable(state.shortlists);
        }
    } catch (err) {
        showToast('Failed to load shortlist history: ' + err.message, 'danger');
    }
}

function renderReportsTable(shortlists) {
    const tbody = document.getElementById('reportsTableBody');
    if (!tbody) return;

    if (shortlists.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9">
                    <div class="empty-state">
                        <i class="bi bi-journal-text"></i>
                        <h5>No Shortlists In Archive</h5>
                        <p>Generate a shortlist for visiting companies to review historical runs.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = shortlists.map(s => {
        const dateStr = s.generatedAt ? s.generatedAt.replace('T', ' ').substring(0, 16) : '-';
        return `
            <tr>
                <td><code>#SH-${s.id}</code></td>
                <td class="text-muted small">${dateStr}</td>
                <td class="fw-bold text-primary">${escapeHtml(s.companyName)}</td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(s.jobRole)}</span></td>
                <td><span class="badge bg-secondary-subtle text-secondary-emphasis">${s.cgpaWeight}/${s.skillsWeight}/${s.certsWeight}</span></td>
                <td>${s.totalEvaluated}</td>
                <td><span class="badge bg-success">${s.eligibleCount}</span></td>
                <td><span class="badge bg-danger">${s.ineligibleCount}</span></td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" onclick="viewHistoryDetails(${s.id})" title="View Details">
                            <i class="bi bi-eye"></i>
                        </button>
                        <a href="/api/shortlists/${s.id}/export/csv" class="btn btn-outline-success" title="Download CSV">
                            <i class="bi bi-file-earmark-spreadsheet"></i>
                        </a>
                        <a href="/api/shortlists/${s.id}/export/json" class="btn btn-outline-info" title="Download JSON">
                            <i class="bi bi-file-earmark-code"></i>
                        </a>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function viewHistoryDetails(id) {
    try {
        const res = await fetch(`/api/shortlists/${id}`);
        if (!res.ok) throw new Error('Shortlist not found');

        const s = await res.json();
        document.getElementById('historyModalTitle').textContent = `Shortlist Session: ${s.companyName} (${s.jobRole})`;
        document.getElementById('historyModalCriteria').innerHTML = `
            <strong>Generated At:</strong> ${s.generatedAt} &nbsp;|&nbsp;
            <strong>Evaluated:</strong> ${s.totalEvaluated} &nbsp;|&nbsp;
            <span class="text-success fw-bold">Eligible: ${s.eligibleCount}</span> &nbsp;|&nbsp;
            <span class="text-danger fw-bold">Ineligible: ${s.ineligibleCount}</span><br>
            <strong>Preserved Criteria Snapshot:</strong> ${escapeHtml(s.criteriaSummary || 'None')}
        `;

        const candidates = s.candidates || [];
        const tbody = document.getElementById('historyModalTableBody');

        tbody.innerHTML = candidates.map(c => `
            <tr>
                <td>${c.rankNumber ? `<span class="badge bg-primary">#${c.rankNumber}</span>` : '-'}</td>
                <td><code>${escapeHtml(c.studentId)}</code></td>
                <td class="fw-semibold">${escapeHtml(c.studentName)}</td>
                <td>${escapeHtml(c.department)}</td>
                <td>${c.cgpa.toFixed(2)}</td>
                <td>${c.activeBacklogs}</td>
                <td><strong>${c.rankingScore > 0 ? c.rankingScore.toFixed(2) : '-'}</strong></td>
                <td>${c.eligible
                    ? '<span class="badge bg-success">Eligible</span>'
                    : '<span class="badge bg-danger">Ineligible</span>'}</td>
                <td class="small text-danger">${escapeHtml(c.rejectionReasons || '')}</td>
            </tr>
        `).join('');

        new bootstrap.Modal(document.getElementById('historyDetailsModal')).show();
    } catch (err) {
        showToast('Error opening archive: ' + err.message, 'danger');
    }
}

// ==============================================================================
// 7. Generic Delete Confirmation and Utilities
// ==============================================================================

function promptDelete(type, id, name) {
    state.deleteTarget = { type, id, name };
    document.getElementById('deleteModalMessage').textContent =
        `Are you sure you want to delete ${type} "${name}"? This action cannot be undone.`;

    const confirmBtn = document.getElementById('btnConfirmDelete');
    confirmBtn.onclick = executeDelete;

    new bootstrap.Modal(document.getElementById('deleteConfirmModal')).show();
}

async function executeDelete() {
    const { type, id } = state.deleteTarget;
    if (!type || !id) return;

    try {
        const url = type === 'student' ? `/api/students/${id}` : `/api/companies/${id}`;
        const res = await fetch(url, { method: 'DELETE' });

        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
            showToast(`${type.charAt(0).toUpperCase() + type.slice(1)} deleted successfully!`, 'success');

            if (type === 'student') await loadStudents();
            if (type === 'company') await loadCompanies();
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to delete record', 'danger');
        }
    } catch (err) {
        showToast('Server error while deleting: ' + err.message, 'danger');
    }
}

function showToast(message, type = 'info') {
    const toastEl = document.getElementById('appToast');
    const toastBody = document.getElementById('toastMessage');

    toastEl.className = `toast align-items-center text-white border-0 bg-${type}`;
    toastBody.textContent = message;

    const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
    toast.show();
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
