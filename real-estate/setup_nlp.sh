#!/bin/bash

# Setup Script for Vietnamese NLP Integration
echo "🚀 Setting up Vietnamese NLP for Real Estate Search System..."

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3.8 or higher."
    exit 1
fi

echo "✅ Python 3 found: $(python3 --version)"

# Check if pip is installed
if ! command -v pip3 &> /dev/null; then
    echo "❌ pip3 is not installed. Please install pip for Python 3."
    exit 1
fi

echo "✅ pip3 found: $(pip3 --version)"

# Create virtual environment (optional but recommended)
if [ "$1" = "--venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv nlp_env
    source nlp_env/bin/activate
    echo "✅ Virtual environment activated"
fi

# Install Python dependencies
echo "📥 Installing Python dependencies..."
pip3 install -r requirements.txt

# Test UndertheSea installation
echo "🧪 Testing UndertheSea installation..."
python3 -c "
import underthesea
print('✅ UndertheSea installed successfully')
print('Version:', underthesea.__version__)

# Test basic functionality
text = 'Tôi muốn tìm căn hộ 2 phòng ngủ ở quận 7'
tokens = underthesea.word_tokenize(text)
print('✅ Tokenization test passed:', tokens[:5])

ner = underthesea.ner(text)
print('✅ NER test passed:', len(ner), 'tokens processed')
"

if [ $? -eq 0 ]; then
    echo "🎉 Vietnamese NLP setup completed successfully!"
    echo ""
    echo "📝 Test the NLP processor with:"
    echo "python3 src/main/resources/nlp/underthesea_processor.py 'Tôi muốn tìm căn hộ 2 phòng ngủ gần trường học dưới 5 tỷ'"
    echo ""
    echo "🚀 Start your Spring Boot application to use Smart Search!"
else
    echo "❌ Setup failed. Please check the error messages above."
    exit 1
fi 