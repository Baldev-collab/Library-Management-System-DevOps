// ===================================================================
// Automated Library Management System - Frontend Interactions
// ===================================================================

document.addEventListener('DOMContentLoaded', function () {
    // Mobile sidebar toggle
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const sidebar = document.querySelector('.app-sidebar');

    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', function () {
            sidebar.classList.toggle('show');
        });
    }

    // Auto-dismiss alerts after 6 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) {
                bsAlert.close();
            }
        }, 6000);
    });

    // Populate Return Book Modal dynamically
    const returnModal = document.getElementById('returnModal');
    if (returnModal) {
        returnModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const issueId = button.getAttribute('data-issue-id');
            const bookTitle = button.getAttribute('data-book-title');
            const memberName = button.getAttribute('data-member-name');
            const dueDate = button.getAttribute('data-due-date');
            const estimatedFee = button.getAttribute('data-late-fee');

            const form = document.getElementById('returnForm');
            form.action = '/issues/return/' + issueId;

            document.getElementById('modalBookTitle').textContent = bookTitle;
            document.getElementById('modalMemberName').textContent = memberName;
            document.getElementById('modalDueDate').textContent = dueDate;
            document.getElementById('modalLateFee').textContent = '$' + (estimatedFee || '0.00');

            const todayInput = document.getElementById('modalReturnDate');
            if (todayInput && !todayInput.value) {
                todayInput.value = new Date().toISOString().split('T')[0];
            }
        });
    }
});
