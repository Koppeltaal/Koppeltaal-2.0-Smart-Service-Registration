/**
 * Dual-List IDP Selector with Drag & Drop functionality
 * Manages ordered selection of Identity Providers with visual drag-and-drop interface
 */
(function() {
  'use strict';

  // Initialize all dual-list selectors on the page
  window.initializeDualListSelectors = function(idpTypes) {
    idpTypes.forEach(idpType => {
      initializeIdpDragDrop(idpType.available, idpType.selected);
    });
  };

  function initializeIdpDragDrop(availableId, selectedId) {
    const availableList = document.getElementById(availableId);
    const selectedList = document.getElementById(selectedId);

    if (!availableList || !selectedList) {
      console.warn('Missing elements for dual-list selector:', { availableId, selectedId });
      return;
    }

    // Hide already selected IDPs from available list
    syncAvailableList(availableList, selectedList);

    // Setup drag & drop for available list items
    availableList.querySelectorAll('li').forEach(item => {
      setupDragListeners(item, availableList, selectedList);
    });

    // Setup drag & drop for selected list items (for reordering)
    selectedList.querySelectorAll('li').forEach(item => {
      setupDragListeners(item, selectedList, selectedList);
      setupCopyButton(item);
    });

    // Setup drop zones
    setupDropZone(availableList, selectedList);
    setupDropZone(selectedList, selectedList);

    // Initialize default labels
    updateDefaultLabels(selectedList);
  }

  function setupDragListeners(item, sourceList, targetList) {
    item.addEventListener('dragstart', (e) => {
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('text/html', item.innerHTML);
      e.dataTransfer.setData('idp-id', item.getAttribute('data-idp-id'));
      e.dataTransfer.setData('source-list', sourceList.id);
      item.classList.add('dragging');
    });

    item.addEventListener('dragend', (e) => {
      item.classList.remove('dragging');
    });
  }

  function setupDropZone(dropZone, selectedList) {
    dropZone.addEventListener('dragover', (e) => {
      const dragging = document.querySelector('.dragging');
      if (!dragging) return;

      // Get the source list ID from the dragging element's parent
      const sourceList = dragging.parentElement;
      if (!sourceList) return;

      const sourceActorType = getActorType(sourceList.id);
      const targetActorType = getActorType(dropZone.id);

      // Only allow drop within same actor type
      if (sourceActorType !== targetActorType) {
        e.dataTransfer.dropEffect = 'none';
        return;
      }

      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';

      const afterElement = getDragAfterElement(dropZone, e.clientY);

      if (afterElement == null) {
        dropZone.appendChild(dragging);
      } else {
        dropZone.insertBefore(dragging, afterElement);
      }

      // Update default labels if moving within or to a selected list
      if (dropZone.id.includes('selected')) {
        updateDefaultLabels(dropZone);
      }

      // If item came from a selected list, update that list too
      if (sourceList && sourceList.id.includes('selected')) {
        updateDefaultLabels(sourceList);
      }

      // If moving to available list, cleanup the item
      if (dropZone.id.includes('available')) {
        cleanupAvailableItem(dragging);
      }
    });

    dropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      const sourceListId = e.dataTransfer.getData('source-list');
      const sourceList = document.getElementById(sourceListId);
      const idpId = e.dataTransfer.getData('idp-id');
      const dragging = document.querySelector('.dragging');

      const sourceActorType = getActorType(sourceListId);
      const targetActorType = getActorType(dropZone.id);

      // Prevent cross-actor-type dragging
      if (sourceActorType !== targetActorType) {
        return;
      }

      // Determine available and selected lists based on IDs
      const availableListId = dropZone.id.includes('available')
        ? dropZone.id
        : dropZone.id.replace('selected', 'available');
      const selectedListId = dropZone.id.includes('selected')
        ? dropZone.id
        : dropZone.id.replace('available', 'selected');
      const availableListEl = document.getElementById(availableListId);
      const selectedListEl = document.getElementById(selectedListId);

      // If moving from available to selected, update styling
      if (sourceListId.includes('available') && dropZone.id.includes('selected')) {
        dragging.style.backgroundColor = '#e0f2f1';
        dragging.style.padding = '8px 10px';

        // Ensure flexbox styling
        dragging.style.display = 'flex';
        dragging.style.alignItems = 'center';

        // Ensure span has class for name
        const nameSpan = dragging.querySelector('span');
        if (nameSpan && !nameSpan.classList.contains('idp-name')) {
          nameSpan.classList.add('idp-name');
        }
        if (nameSpan) {
          nameSpan.style.flex = '1';
        }

        // Add copy button if not exists and logical ID is available
        const logicalId = dragging.getAttribute('data-logical-id');
        if (!dragging.querySelector('.copy-idp') && !dragging.querySelector('.copy-idp-disabled')) {
          if (logicalId) {
            const copyBtn = document.createElement('a');
            copyBtn.href = '#!';
            copyBtn.className = 'secondary-content copy-idp tooltipped';
            copyBtn.setAttribute('data-logical-id', logicalId);
            copyBtn.setAttribute('data-position', 'top');
            copyBtn.setAttribute('data-tooltip', 'Copy Logical Identifier');
            copyBtn.style.marginLeft = '8px';
            copyBtn.style.position = 'static';
            copyBtn.innerHTML = '<i class="material-icons" style="font-size: 18px;">content_copy</i>';
            dragging.appendChild(copyBtn);
            setupCopyButton(dragging);
            M.Tooltip.init(copyBtn);
          } else {
            const disabledSpan = document.createElement('span');
            disabledSpan.className = 'secondary-content copy-idp-disabled tooltipped';
            disabledSpan.setAttribute('data-position', 'top');
            disabledSpan.setAttribute('data-tooltip', 'Geen logical identifier toegekend');
            disabledSpan.style.marginLeft = '8px';
            disabledSpan.style.position = 'static';
            disabledSpan.style.color = '#ccc';
            disabledSpan.innerHTML = '<i class="material-icons" style="font-size: 18px;">content_copy</i>';
            dragging.appendChild(disabledSpan);
            M.Tooltip.init(disabledSpan);
          }
        }
      }

      // If moving from selected to available, remove styling and buttons
      if (sourceListId.includes('selected') && dropZone.id.includes('available')) {
        cleanupAvailableItem(dragging);
      }

      // Hide/show items in available list based on what's selected
      syncAvailableList(availableListEl, selectedListEl);

      // Update default labels
      updateDefaultLabels(selectedListEl);
    });
  }

  function setupCopyButton(item) {
    const copyBtn = item.querySelector('.copy-idp');
    if (copyBtn) {
      copyBtn.addEventListener('click', (e) => {
        e.preventDefault();
        const logicalId = copyBtn.getAttribute('data-logical-id');
        if (logicalId) {
          copyToClipboard(logicalId);
        }
      });
    }
  }

  function getDragAfterElement(container, y) {
    const draggableElements = [...container.querySelectorAll('li:not(.dragging)')];

    return draggableElements.reduce((closest, child) => {
      const box = child.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;

      if (offset < 0 && offset > closest.offset) {
        return { offset: offset, element: child };
      } else {
        return closest;
      }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
  }

  function syncAvailableList(availableList, selectedList) {
    const selectedIds = Array.from(selectedList.querySelectorAll('li')).map(item => item.getAttribute('data-idp-id'));

    // Track which IDs we've seen to remove duplicates
    const seenIds = new Set();

    availableList.querySelectorAll('li').forEach(item => {
      const idpId = item.getAttribute('data-idp-id');

      // If this is a duplicate (we've seen this ID already), remove it
      if (seenIds.has(idpId)) {
        item.remove();
        return;
      }

      seenIds.add(idpId);

      // Hide if selected, show if not selected
      if (selectedIds.includes(idpId)) {
        item.style.display = 'none';
      } else {
        // Ensure proper flexbox styling when showing
        item.style.display = 'flex';
        item.style.alignItems = 'center';
      }
    });
  }

  function getActorType(listId) {
    if (listId.includes('patient')) return 'patient';
    if (listId.includes('practitioner')) return 'practitioner';
    if (listId.includes('relatedperson')) return 'relatedperson';
    return null;
  }

  function updateDefaultLabels(selectedList) {
    const items = selectedList.querySelectorAll('li');
    items.forEach((item, index) => {
      // Ensure consistent styling for selected items
      item.style.backgroundColor = '#e0f2f1';
      item.style.padding = '8px 10px';
      item.style.cursor = 'move';
      item.style.display = 'flex';
      item.style.alignItems = 'center';

      // Ensure span has idp-name class and flex styling
      const nameSpan = item.querySelector('span');
      if (nameSpan && !nameSpan.classList.contains('idp-name')) {
        nameSpan.classList.add('idp-name');
      }
      if (nameSpan) {
        nameSpan.style.flex = '1';
      }

      // Update name with default label
      if (nameSpan) {
        const baseName = item.getAttribute('data-idp-name');
        if (index === 0 && items.length > 0) {
          nameSpan.textContent = baseName + ' (default)';
        } else {
          nameSpan.textContent = baseName;
        }
      }

      // Ensure all items in selected list have copy button (or disabled indicator)
      const logicalId = item.getAttribute('data-logical-id');

      if (!item.querySelector('.copy-idp') && !item.querySelector('.copy-idp-disabled')) {
        if (logicalId) {
          const copyBtn = document.createElement('a');
          copyBtn.href = '#!';
          copyBtn.className = 'secondary-content copy-idp tooltipped';
          copyBtn.setAttribute('data-logical-id', logicalId);
          copyBtn.setAttribute('data-position', 'top');
          copyBtn.setAttribute('data-tooltip', 'Copy Logical Identifier');
          copyBtn.style.marginLeft = '8px';
          copyBtn.style.position = 'static';
          copyBtn.innerHTML = '<i class="material-icons" style="font-size: 18px;">content_copy</i>';
          item.appendChild(copyBtn);
          setupCopyButton(item);
          M.Tooltip.init(copyBtn);
        } else {
          const disabledSpan = document.createElement('span');
          disabledSpan.className = 'secondary-content copy-idp-disabled tooltipped';
          disabledSpan.setAttribute('data-position', 'top');
          disabledSpan.setAttribute('data-tooltip', 'Geen logical identifier toegekend');
          disabledSpan.style.marginLeft = '8px';
          disabledSpan.style.position = 'static';
          disabledSpan.style.color = '#ccc';
          disabledSpan.innerHTML = '<i class="material-icons" style="font-size: 18px;">content_copy</i>';
          item.appendChild(disabledSpan);
          M.Tooltip.init(disabledSpan);
        }
      }
    });
  }

  function cleanupAvailableItem(item) {
    // Reset styling
    item.style.backgroundColor = '';
    item.style.padding = '8px 10px';
    item.style.display = 'flex';
    item.style.alignItems = 'center';

    // Remove copy button or disabled indicator
    const copyBtn = item.querySelector('.copy-idp');
    if (copyBtn) copyBtn.remove();
    const disabledSpan = item.querySelector('.copy-idp-disabled');
    if (disabledSpan) disabledSpan.remove();

    // Reset name (remove "(default)" if present)
    const nameSpan = item.querySelector('.idp-name');
    if (nameSpan) {
      const baseName = item.getAttribute('data-idp-name');
      nameSpan.textContent = baseName;
      nameSpan.style.flex = '';
    }
  }

  function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
      M.toast({ html: 'Logical identifier copied to clipboard!', classes: 'green' });
    }).catch(err => {
      M.toast({ html: 'Failed to copy: ' + err, classes: 'red' });
    });
  }
})();
